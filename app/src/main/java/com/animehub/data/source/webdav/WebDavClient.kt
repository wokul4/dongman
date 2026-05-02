package com.animehub.data.source.webdav

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

class WebDavClient(
    private val config: WebDavConfig,
    private val httpClient: OkHttpClient = DEFAULT_CLIENT
) {
    companion object {
        private val DEFAULT_CLIENT = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        private val PROPFIND_REQUEST_BODY = """<?xml version="1.0"?>
<d:propfind xmlns:d="DAV:">
    <d:prop>
        <d:displayname/>
        <d:getcontentlength/>
        <d:getcontenttype/>
        <d:getlastmodified/>
        <d:resourcetype/>
    </d:prop>
</d:propfind>""".toRequestBody("application/xml".toMediaType())
    }

    sealed class WebDavResult<out T> {
        data class Success<T>(val data: T) : WebDavResult<T>()
        data class Error(val message: String) : WebDavResult<Nothing>()
    }

    fun testConnection(): WebDavResult<Unit> {
        return try {
            val response = executePropfind(config.normalizedBaseUrl + config.normalizedRootPath, depth = 0)
            when (response.code) {
                in 200..299 -> WebDavResult.Success(Unit)
                401, 403 -> WebDavResult.Error("认证失败，请检查用户名和密码")
                404 -> WebDavResult.Error("路径不存在")
                405 -> WebDavResult.Error("服务器不支持 WebDAV PROPFIND")
                in 500..599 -> WebDavResult.Error("服务器错误: ${response.code}")
                else -> WebDavResult.Error("未知错误: ${response.code}")
            }
        } catch (e: java.net.UnknownHostException) {
            WebDavResult.Error("无法解析服务器地址，请检查 URL")
        } catch (e: java.net.ConnectException) {
            WebDavResult.Error("连接失败，请检查地址和端口")
        } catch (e: java.net.SocketTimeoutException) {
            WebDavResult.Error("连接超时，请检查网络或服务器状态")
        } catch (e: IllegalArgumentException) {
            WebDavResult.Error("地址格式不正确")
        } catch (e: Exception) {
            WebDavResult.Error("连接失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    fun listDirectory(path: String): WebDavResult<List<WebDavItem>> {
        return try {
            val fullUrl = buildUrl(path)
            val response = executePropfind(fullUrl, depth = 1)
            if (!response.isSuccessful) {
                return when (response.code) {
                    401, 403 -> WebDavResult.Error("认证失败")
                    404 -> WebDavResult.Error("目录不存在")
                    else -> WebDavResult.Error("服务器返回 ${response.code}")
                }
            }
            val body = response.body?.string() ?: return WebDavResult.Error("服务器返回空响应")
            val items = parseMultiStatus(body, path)
            WebDavResult.Success(items)
        } catch (e: Exception) {
            WebDavResult.Error("读取目录失败: ${e.localizedMessage ?: "未知错误"}")
        }
    }

    fun buildPlayableUrl(href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        val origin = config.normalizedBaseUrl.let { url ->
            try {
                val parsed = java.net.URI(url)
                val userInfo = config.username?.let { user ->
                    config.password?.let { pass ->
                        java.net.URLEncoder.encode(user, "UTF-8") + ":" +
                                java.net.URLEncoder.encode(pass, "UTF-8") + "@"
                    }
                } ?: ""
                "${parsed.scheme}://$userInfo${parsed.host}${if (parsed.port > 0) ":${parsed.port}" else ""}"
            } catch (_: Exception) { url }
        }
        val cleanHref = if (href.startsWith("/")) href else "/$href"
        return origin.trimEnd('/') + cleanHref
    }

    fun buildAuthHeader(): Map<String, String> {
        val user = config.username ?: return emptyMap()
        val pass = config.password ?: return emptyMap()
        if (user.isBlank() && pass.isBlank()) return emptyMap()
        return mapOf("Authorization" to Credentials.basic(user, pass))
    }

    private fun buildUrl(path: String): String {
        val base = config.normalizedBaseUrl
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return base + normalizedPath
    }

    private fun executePropfind(url: String, depth: Int): okhttp3.Response {
        val request = Request.Builder()
            .url(url)
            .method("PROPFIND", PROPFIND_REQUEST_BODY)
            .header("Depth", depth.toString())
            .apply {
                val auth = buildAuthHeader()
                auth.forEach { (k, v) -> header(k, v) }
            }
            .build()
        return httpClient.newCall(request).execute()
    }

    internal fun parseMultiStatus(xml: String, dirPath: String): List<WebDavItem> {
        val items = mutableListOf<WebDavItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.setNamespaceAware(true)
        val parser = factory.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(xml))

        var currentHref: String? = null
        var currentName: String? = null
        var currentSize: Long? = null
        var currentType: String? = null
        var currentModified: String? = null
        var isCollection = false
        var insideResponse = false
        var insideProp = false
        var insidePropstat = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "response" -> {
                            insideResponse = true
                            currentHref = null; currentName = null
                            currentSize = null; currentType = null
                            currentModified = null; isCollection = false
                        }
                        "href" -> if (insideResponse) currentHref = parser.nextText().trim()
                        "displayname" -> if (insideProp) currentName = parser.nextText().trim()
                        "getcontentlength" -> if (insideProp) {
                            currentSize = parser.nextText().trim().toLongOrNull()
                        }
                        "getcontenttype" -> if (insideProp) currentType = parser.nextText().trim()
                        "getlastmodified" -> if (insideProp) currentModified = parser.nextText().trim()
                        "collection" -> if (insideProp) isCollection = true
                        "prop" -> if (insidePropstat) insideProp = true
                        "propstat" -> if (insideResponse) insidePropstat = true
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "response" -> {
                            if (insideResponse && currentHref != null) {
                                val href = currentHref
                                val dirDecoded = URLDecoder.decode(dirPath, "UTF-8")
                                val itemDecoded = URLDecoder.decode(href, "UTF-8")
                                val decodedPath = itemDecoded.trimEnd('/')
                                val name = when {
                                    !currentName.isNullOrBlank() -> currentName!!
                                    else -> decodedPath.substringAfterLast('/')
                                }

                                // Skip the current directory listing
                                val dirHref = if (dirPath.endsWith("/")) dirPath else "$dirPath/"
                                if (href != dirHref && href != "${dirHref}/") {
                                    items.add(
                                        WebDavItem(
                                            name = name,
                                            path = decodedPath,
                                            href = href,
                                            isDirectory = isCollection,
                                            sizeBytes = currentSize,
                                            lastModified = parseHttpDate(currentModified),
                                            contentType = currentType
                                        )
                                    )
                                }
                            }
                            currentHref = null; insideResponse = false
                            insidePropstat = false; insideProp = false
                        }
                        "propstat" -> insidePropstat = false
                        "prop" -> insideProp = false
                    }
                }
            }
            parser.next()
        }
        return items
    }

    private fun parseHttpDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", java.util.Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("GMT")
            sdf.parse(dateStr)?.time
        } catch (_: Exception) {
            null
        }
    }
}
