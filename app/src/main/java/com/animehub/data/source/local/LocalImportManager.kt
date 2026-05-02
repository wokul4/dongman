package com.animehub.data.source.local

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.animehub.data.local.dao.AnimeDao
import com.animehub.data.local.dao.EpisodeDao
import com.animehub.data.local.entity.AnimeEntity
import com.animehub.data.local.entity.EpisodeEntity
import com.animehub.util.FileNameParser
import com.animehub.util.VideoFileDetector
import com.animehub.util.VideoMetadataReader

class LocalImportManager(
    private val context: Context,
    private val animeDao: AnimeDao,
    private val episodeDao: EpisodeDao
) {

    data class ImportResult(
        val animeCount: Int,
        val episodeCount: Int,
        val failedCount: Int
    )

    suspend fun importSingleVideo(uri: Uri): ImportResult {
        persistUriPermission(uri)
        val info = resolveUriInfo(uri) ?: return ImportResult(0, 0, 1)
        val animeId = info.displayName.substringBeforeLast(".")
        val now = System.currentTimeMillis()

        val episodeNum = FileNameParser.parseEpisodeNumber(info.displayName)
        val animeTitle = FileNameParser.extractTitle(info.displayName)

        val animeEntity = AnimeEntity(
            id = animeId,
            sourceId = "local_file",
            title = animeTitle,
            coverUrl = null,
            description = null,
            updatedAt = now
        )
        val durationMs = VideoMetadataReader.readDurationMs(context, uri)
        val episodeEntity = EpisodeEntity(
            id = uri.toString(),
            animeId = animeId,
            sourceId = "local_file",
            title = info.displayName,
            episodeNumber = episodeNum ?: 1f,
            durationMs = durationMs,
            playableUrl = uri.toString()
        )

        animeDao.insertAll(listOf(animeEntity))
        episodeDao.insertAll(listOf(episodeEntity))
        return ImportResult(1, 1, 0)
    }

    suspend fun importMultipleVideos(uris: List<Uri>): ImportResult {
        if (uris.isEmpty()) return ImportResult(0, 0, 0)

        val resolved = uris.mapNotNull { uri ->
            persistUriPermission(uri)
            val info = resolveUriInfo(uri)
            if (info != null && VideoFileDetector.isVideoFile(info.displayName)) {
                uri to info
            } else null
        }

        if (resolved.isEmpty()) return ImportResult(0, 0, uris.size)

        val groupName = "本地视频"
        val now = System.currentTimeMillis()

        val animeEntity = AnimeEntity(
            id = groupName,
            sourceId = "local_file",
            title = groupName,
            coverUrl = null,
            description = "从本地导入 ${resolved.size} 个视频",
            updatedAt = now
        )

        val sorted = resolved.sortedBy { (_, info) -> info.displayName }
        val episodeEntities = sorted.mapIndexed { index, (uri, info) ->
            val episodeNum = FileNameParser.parseEpisodeNumber(info.displayName) ?: (index + 1).toFloat()
            val durationMs = VideoMetadataReader.readDurationMs(context, uri)
            EpisodeEntity(
                id = uri.toString(),
                animeId = groupName,
                sourceId = "local_file",
                title = info.displayName,
                episodeNumber = episodeNum,
                durationMs = durationMs,
                playableUrl = uri.toString()
            )
        }

        animeDao.insertAll(listOf(animeEntity))
        episodeDao.insertAll(episodeEntities)
        return ImportResult(1, episodeEntities.size, uris.size - resolved.size)
    }

    suspend fun importFromTreeUri(treeUri: Uri, animeTitle: String): ImportResult {
        persistTreeUriPermission(treeUri)
        val docId = DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri, docId
        )

        val videoUris = mutableListOf<Pair<Uri, String>>()

        try {
            context.contentResolver.query(childrenUri, null, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docIdCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

                    val docUri = if (docIdCol >= 0) {
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(docIdCol))
                    } else null

                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else ""
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else ""

                    if (docUri != null && VideoFileDetector.isVideoMimeType(mime)) {
                        persistUriPermission(docUri)
                        videoUris.add(docUri to name)
                    }
                }
            }
        } catch (e: Exception) {
            return ImportResult(0, 0, 0)
        }

        if (videoUris.isEmpty()) return ImportResult(0, 0, 0)

        val now = System.currentTimeMillis()
        val animeId = animeTitle

        val animeEntity = AnimeEntity(
            id = animeId,
            sourceId = "local_file",
            title = animeTitle,
            coverUrl = null,
            description = "从 ${animeTitle} 目录导入 ${videoUris.size} 个视频",
            updatedAt = now
        )

        val sorted = videoUris.sortedBy { (_, name) -> name }
        val episodeEntities = sorted.mapIndexed { index, (uri, name) ->
            val episodeNum = FileNameParser.parseEpisodeNumber(name) ?: (index + 1).toFloat()
            val durationMs = VideoMetadataReader.readDurationMs(context, uri)
            EpisodeEntity(
                id = uri.toString(),
                animeId = animeId,
                sourceId = "local_file",
                title = name,
                episodeNumber = episodeNum,
                durationMs = durationMs,
                playableUrl = uri.toString()
            )
        }

        animeDao.insertAll(listOf(animeEntity))
        episodeDao.insertAll(episodeEntities)
        return ImportResult(1, episodeEntities.size, 0)
    }

    private fun resolveUriInfo(uri: Uri): UriInfo? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)
                    else uri.lastPathSegment ?: "Unknown"
                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    UriInfo(name, uri, context.contentResolver.getType(uri), size)
                } else null
            }
        } catch (e: Exception) {
            UriInfo(uri.lastPathSegment ?: "Unknown", uri, null, 0L)
        }
    }

    private fun persistUriPermission(uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { }
    }

    private fun persistTreeUriPermission(treeUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) { }
    }

    data class UriInfo(
        val displayName: String,
        val uri: Uri,
        val mimeType: String?,
        val size: Long
    )
}
