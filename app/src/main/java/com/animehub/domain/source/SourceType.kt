package com.animehub.domain.source

enum class SourceType(val displayName: String) {
    LOCAL_FILE("本地视频"),
    WEBDAV("WebDAV"),
    RSS("RSS/Atom"),
    LICENSED_API("授权 API"),
    OFFICIAL_DEEPLINK("官方 Deep Link")
}
