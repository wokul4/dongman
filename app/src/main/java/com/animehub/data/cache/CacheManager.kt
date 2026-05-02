package com.animehub.data.cache

import android.content.Context
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import java.io.File

class CacheManager(private val context: Context) {

    data class CacheInfo(
        val totalSizeBytes: Long,
        val cacheDirSize: Long,
        val coilCacheSize: Long
    ) {
        val totalFormatted: String get() = formatBytes(totalSizeBytes)
        val cacheDirFormatted: String get() = formatBytes(cacheDirSize)
        val coilCacheFormatted: String get() = formatBytes(coilCacheSize)
    }

    @OptIn(ExperimentalCoilApi::class)
    fun getCacheInfo(): CacheInfo {
        val cacheDirSize = calculateDirSize(context.cacheDir)
        val coilCacheSize = try {
            val imageLoader = Coil.imageLoader(context)
            val diskCache = imageLoader.diskCache
            diskCache?.let { calculateDirSize(it.directory.toFile()) } ?: 0L
        } catch (_: Exception) {
            0L
        }
        return CacheInfo(
            totalSizeBytes = cacheDirSize + coilCacheSize,
            cacheDirSize = cacheDirSize,
            coilCacheSize = coilCacheSize
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun clearAll(): Result<Unit> {
        return try {
            clearDirectory(context.cacheDir)
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            } catch (_: Exception) { }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun clearDirectory(dir: File) {
        if (!dir.exists()) return
        dir.walkTopDown()
            .sortedByDescending { it.isDirectory }
            .forEach { it.delete() }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
                else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
            }
        }
    }
}
