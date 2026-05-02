package com.animehub.data.source.webdav

import kotlinx.serialization.Serializable

@Serializable
data class WebDavConfig(
    val baseUrl: String,
    val displayName: String,
    val username: String? = null,
    val password: String? = null,
    val rootPath: String = "/"
) {
    val normalizedBaseUrl: String
        get() = baseUrl.trimEnd('/')

    val normalizedRootPath: String
        get() = "/" + rootPath.trim('/')
}

data class WebDavItem(
    val name: String,
    val path: String,
    val href: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val lastModified: Long? = null,
    val contentType: String? = null
)
