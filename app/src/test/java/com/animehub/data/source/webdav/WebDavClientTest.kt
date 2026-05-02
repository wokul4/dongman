package com.animehub.data.source.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavClientTest {

    private val sampleXml = """<?xml version="1.0"?>
<d:multistatus xmlns:d="DAV:">
  <d:response>
    <d:href>/remote.php/dav/files/user/Videos/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Videos</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
        <d:getlastmodified>Mon, 12 Jan 2024 10:00:00 GMT</d:getlastmodified>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
  <d:response>
    <d:href>/remote.php/dav/files/user/Videos/Subdir/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Subdir</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
        <d:getlastmodified>Mon, 12 Jan 2024 10:00:00 GMT</d:getlastmodified>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
  <d:response>
    <d:href>/remote.php/dav/files/user/Videos/test.mp4</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>test.mp4</d:displayname>
        <d:getcontentlength>12345678</d:getcontentlength>
        <d:getcontenttype>video/mp4</d:getcontenttype>
        <d:getlastmodified>Mon, 12 Jan 2024 10:00:00 GMT</d:getlastmodified>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>
</d:multistatus>"""

    private val client = WebDavClient(
        WebDavConfig(baseUrl = "https://example.com", displayName = "test")
    )

    @Test
    fun `parse directory listing`() {
        val items = client.parseMultiStatus(sampleXml, "/remote.php/dav/files/user/Videos")
        assertEquals(2, items.size)
    }

    @Test
    fun `parse directories correctly`() {
        val items = client.parseMultiStatus(sampleXml, "/remote.php/dav/files/user/Videos")
        val dirs = items.filter { it.isDirectory }
        assertEquals(1, dirs.size)
        assertEquals("Subdir", dirs[0].name)
    }

    @Test
    fun `parse files correctly`() {
        val items = client.parseMultiStatus(sampleXml, "/remote.php/dav/files/user/Videos")
        val files = items.filter { !it.isDirectory }
        assertEquals(1, files.size)
        assertEquals("test.mp4", files[0].name)
        assertEquals(12345678L, files[0].sizeBytes)
    }

    @Test
    fun `parse file content type`() {
        val items = client.parseMultiStatus(sampleXml, "/remote.php/dav/files/user/Videos")
        val file = items.find { !it.isDirectory }
        assertEquals("video/mp4", file?.contentType)
    }

    @Test
    fun `empty multistatus returns empty list`() {
        val emptyXml = """<?xml version="1.0"?><d:multistatus xmlns:d="DAV:"></d:multistatus>"""
        val items = client.parseMultiStatus(emptyXml, "/")
        assertTrue(items.isEmpty())
    }

    @Test
    fun `buildPlayableUrl from absolute href`() {
        val url = client.buildPlayableUrl("https://other.com/video.mp4")
        assertEquals("https://other.com/video.mp4", url)
    }

    @Test
    fun `buildPlayableUrl from server absolute path`() {
        val client2 = WebDavClient(
            WebDavConfig(baseUrl = "https://example.com/remote.php/dav/files/user", displayName = "test")
        )
        val url = client2.buildPlayableUrl("/remote.php/dav/files/user/Videos/test.mp4")
        assertEquals("https://example.com/remote.php/dav/files/user/Videos/test.mp4", url)
    }

    @Test
    fun `buildPlayableUrl with trailing slash base`() {
        val client2 = WebDavClient(
            WebDavConfig(baseUrl = "https://example.com/", displayName = "test")
        )
        val url = client2.buildPlayableUrl("/videos/test.mp4")
        assertEquals("https://example.com/videos/test.mp4", url)
    }

    @Test
    fun `auth headers with credentials`() {
        val client2 = WebDavClient(
            WebDavConfig(baseUrl = "https://example.com", username = "user", password = "pass", displayName = "test")
        )
        val headers = client2.buildAuthHeader()
        assertTrue(headers.containsKey("Authorization"))
        assertTrue(headers["Authorization"]?.startsWith("Basic ") == true)
    }

    @Test
    fun `auth headers without credentials`() {
        val headers = client.buildAuthHeader()
        assertTrue(headers.isEmpty())
    }
}
