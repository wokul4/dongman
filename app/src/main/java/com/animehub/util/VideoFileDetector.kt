package com.animehub.util

object VideoFileDetector {

    private val videoExtensions = setOf(
        "mp4", "mkv", "webm", "m4v", "avi", "mov",
        "3gp", "ts", "flv", "wmv", "mpg", "mpeg"
    )

    private val videoMimeTypes = setOf(
        "video/mp4", "video/x-matroska", "video/webm",
        "video/m4v", "video/3gpp", "video/quicktime",
        "video/avi", "video/x-msvideo", "video/x-ms-wmv",
        "video/mpeg", "video/mp2t"
    )

    fun isVideoFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in videoExtensions
    }

    fun isVideoMimeType(mimeType: String?): Boolean {
        return mimeType != null && (mimeType in videoMimeTypes || mimeType.startsWith("video/"))
    }

    fun getSupportedExtensions(): Array<String> {
        return videoExtensions.toTypedArray()
    }

    fun getSupportedMimeTypes(): Array<String> {
        return videoMimeTypes.toTypedArray()
    }
}
