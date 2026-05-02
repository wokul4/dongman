package com.animehub.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoMetadataReader {

    fun readDurationMs(context: Context, uri: Uri): Long? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )
            retriever.release()
            durationStr?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }
}
