package com.animehub.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFileDetectorTest {

    @Test
    fun `video extensions are detected`() {
        assertTrue(VideoFileDetector.isVideoFile("video.mp4"))
        assertTrue(VideoFileDetector.isVideoFile("video.mkv"))
        assertTrue(VideoFileDetector.isVideoFile("video.webm"))
        assertTrue(VideoFileDetector.isVideoFile("video.m4v"))
        assertTrue(VideoFileDetector.isVideoFile("video.avi"))
        assertTrue(VideoFileDetector.isVideoFile("video.mov"))
    }

    @Test
    fun `non-video extensions are rejected`() {
        assertFalse(VideoFileDetector.isVideoFile("file.txt"))
        assertFalse(VideoFileDetector.isVideoFile("image.jpg"))
        assertFalse(VideoFileDetector.isVideoFile("doc.pdf"))
        assertFalse(VideoFileDetector.isVideoFile("data.json"))
    }

    @Test
    fun `case insensitive extension matching`() {
        assertTrue(VideoFileDetector.isVideoFile("video.MP4"))
        assertTrue(VideoFileDetector.isVideoFile("video.MKV"))
        assertTrue(VideoFileDetector.isVideoFile("video.WebM"))
    }

    @Test
    fun `files without extension are not video`() {
        assertFalse(VideoFileDetector.isVideoFile("README"))
        assertFalse(VideoFileDetector.isVideoFile("Makefile"))
    }

    @Test
    fun `video mime types are detected`() {
        assertTrue(VideoFileDetector.isVideoMimeType("video/mp4"))
        assertTrue(VideoFileDetector.isVideoMimeType("video/x-matroska"))
        assertTrue(VideoFileDetector.isVideoMimeType("video/webm"))
        assertTrue(VideoFileDetector.isVideoMimeType("video/quicktime"))
        assertTrue(VideoFileDetector.isVideoMimeType("video/avi"))
    }

    @Test
    fun `non-video mime types are rejected`() {
        assertFalse(VideoFileDetector.isVideoMimeType("text/plain"))
        assertFalse(VideoFileDetector.isVideoMimeType("image/jpeg"))
        assertFalse(VideoFileDetector.isVideoMimeType("application/pdf"))
        assertFalse(VideoFileDetector.isVideoMimeType("application/json"))
    }

    @Test
    fun `null mime type is not video`() {
        assertFalse(VideoFileDetector.isVideoMimeType(null))
    }

    @Test
    fun `supported mime types are not empty`() {
        assertTrue(VideoFileDetector.getSupportedMimeTypes().isNotEmpty())
        assertTrue(VideoFileDetector.getSupportedExtensions().isNotEmpty())
    }
}
