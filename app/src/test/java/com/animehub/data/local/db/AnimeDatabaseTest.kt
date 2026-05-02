package com.animehub.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.animehub.data.local.entity.AnimeEntity
import com.animehub.data.local.entity.EpisodeEntity
import com.animehub.data.local.entity.FavoriteEntity
import com.animehub.data.local.entity.WatchHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnimeDatabaseTest {

    private lateinit var db: AnimeDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AnimeDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and query anime`() = runBlocking {
        val anime = AnimeEntity(
            id = "test_anime_1",
            sourceId = "test_source",
            title = "测试番剧",
            coverUrl = null,
            description = "测试描述",
            updatedAt = 1000L
        )
        db.animeDao().insertAll(listOf(anime))

        val result = db.animeDao().getById("test_anime_1", "test_source")
        assertNotNull(result)
        assertEquals("测试番剧", result?.title)
        assertEquals("test_source", result?.sourceId)
    }

    @Test
    fun `insert and query episodes`() = runBlocking {
        val anime = AnimeEntity(
            id = "ep_test_anime",
            sourceId = "test_source",
            title = "剧集测试",
            coverUrl = null,
            description = null,
            updatedAt = 1000L
        )
        db.animeDao().insertAll(listOf(anime))

        val episodes = listOf(
            EpisodeEntity(
                id = "ep1", animeId = "ep_test_anime",
                sourceId = "test_source", title = "第一集",
                episodeNumber = 1f, durationMs = 600000L, playableUrl = "content://video/1"
            ),
            EpisodeEntity(
                id = "ep2", animeId = "ep_test_anime",
                sourceId = "test_source", title = "第二集",
                episodeNumber = 2f, durationMs = 600000L, playableUrl = "content://video/2"
            )
        )
        db.episodeDao().insertAll(episodes)

        val results = db.episodeDao().getByAnimeId("ep_test_anime", "test_source")
        assertEquals(2, results.size)
        assertEquals("第一集", results[0].title)
    }

    @Test
    fun `insert and query watch history`() = runBlocking {
        val history = WatchHistoryEntity(
            episodeId = "ep1",
            animeId = "anime1",
            sourceId = "src1",
            progressMs = 30000L,
            durationMs = 600000L,
            lastWatchedAt = 2000L
        )
        db.watchHistoryDao().insert(history)

        val result = db.watchHistoryDao().getById("ep1", "anime1", "src1")
        assertNotNull(result)
        assertEquals(30000L, result?.progressMs)
    }

    @Test
    fun `insert and toggle favorite`() = runBlocking {
        db.favoriteDao().insert(
            FavoriteEntity(animeId = "fav1", sourceId = "src1", createdAt = 1000L)
        )

        var found = db.favoriteDao().getById("fav1", "src1")
        assertNotNull(found)

        db.favoriteDao().delete("fav1", "src1")

        found = db.favoriteDao().getById("fav1", "src1")
        assertNull(found)
    }

    @Test
    fun `search anime by title`() = runBlocking {
        val animeList = listOf(
            AnimeEntity(id = "a1", sourceId = "s1", title = "火影忍者", coverUrl = null, description = null, updatedAt = 1L),
            AnimeEntity(id = "a2", sourceId = "s1", title = "海贼王", coverUrl = null, description = null, updatedAt = 2L),
            AnimeEntity(id = "a3", sourceId = "s1", title = "火影忍者剧场版", coverUrl = null, description = null, updatedAt = 3L)
        )
        db.animeDao().insertAll(animeList)

        val results = db.animeDao().searchByTitle("火影")
        assertEquals(2, results.size)
    }
}
