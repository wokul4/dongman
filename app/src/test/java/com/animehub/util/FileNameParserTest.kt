package com.animehub.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileNameParserTest {

    @Test
    fun `plain number prefix returns episode number`() {
        assertEquals(1f, FileNameParser.parseEpisodeNumber("01.mp4"))
        assertEquals(12f, FileNameParser.parseEpisodeNumber("12.mp4"))
        assertEquals(99f, FileNameParser.parseEpisodeNumber("99.mkv"))
    }

    @Test
    fun `chinese episode format`() {
        assertEquals(1f, FileNameParser.parseEpisodeNumber("第01集.mp4"))
        assertEquals(12f, FileNameParser.parseEpisodeNumber("第12集.mkv"))
        assertEquals(5f, FileNameParser.parseEpisodeNumber("第5话.webm"))
    }

    @Test
    fun `SxxExx format extracts episode number`() {
        assertEquals(3f, FileNameParser.parseEpisodeNumber("S01E03.mkv"))
        assertEquals(10f, FileNameParser.parseEpisodeNumber("S02E10.mp4"))
        assertEquals(1f, FileNameParser.parseEpisodeNumber("s01e01.webm"))
    }

    @Test
    fun `Episode prefix format`() {
        assertEquals(12f, FileNameParser.parseEpisodeNumber("Episode 12.webm"))
        assertEquals(5f, FileNameParser.parseEpisodeNumber("Ep05.mp4"))
        assertEquals(8f, FileNameParser.parseEpisodeNumber("ep.08.mkv"))
    }

    @Test
    fun `unparseable names return null`() {
        assertNull(FileNameParser.parseEpisodeNumber("Movie.mp4"))
        assertNull(FileNameParser.parseEpisodeNumber("SomeVideo.mkv"))
        assertNull(FileNameParser.parseEpisodeNumber("favorite_scene.webm"))
    }

    @Test
    fun `extract title removes extension`() {
        assertEquals("01", FileNameParser.extractTitle("01.mp4"))
        assertEquals("第01集", FileNameParser.extractTitle("第01集.mp4"))
        assertEquals("S01E03", FileNameParser.extractTitle("S01E03.mkv"))
        assertEquals("My Movie", FileNameParser.extractTitle("My Movie.mkv"))
    }

    @Test
    fun `bracket format extracts episode number`() {
        assertEquals(1f, FileNameParser.parseEpisodeNumber("[01].mp4"))
        assertEquals(12f, FileNameParser.parseEpisodeNumber("[12] title.mkv"))
    }

    @Test
    fun `E prefix format`() {
        assertEquals(5f, FileNameParser.parseEpisodeNumber("E05.mp4"))
        assertEquals(3f, FileNameParser.parseEpisodeNumber("E03 title.mkv"))
    }

    @Test
    fun `number dash title format`() {
        assertEquals(12f, FileNameParser.parseEpisodeNumber("12 - title.mkv"))
        assertEquals(7f, FileNameParser.parseEpisodeNumber("07 - episode.mkv"))
    }

    @Test
    fun `empty and edge cases`() {
        assertNull(FileNameParser.parseEpisodeNumber(""))
        assertNull(FileNameParser.parseEpisodeNumber("."))
        assertNull(FileNameParser.parseEpisodeNumber(".mp4"))
    }
}
