package com.animehub

import android.app.Application
import com.animehub.data.datastore.SettingsDataStore
import com.animehub.data.local.db.AnimeDatabase
import com.animehub.data.repository.AnimeRepositoryImpl
import com.animehub.data.repository.FavoriteRepositoryImpl
import com.animehub.data.repository.SourceRepositoryImpl
import com.animehub.data.repository.WatchHistoryRepositoryImpl
import com.animehub.data.source.SourceManager

class AnimeHubApplication : Application() {

    lateinit var database: AnimeDatabase
        private set
    lateinit var sourceManager: SourceManager
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var animeRepository: AnimeRepositoryImpl
        private set
    lateinit var sourceRepository: SourceRepositoryImpl
        private set
    lateinit var watchHistoryRepository: WatchHistoryRepositoryImpl
        private set
    lateinit var favoriteRepository: FavoriteRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        database = AnimeDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)

        sourceManager = SourceManager(
            context = this,
            sourceDao = database.sourceDao(),
            animeDao = database.animeDao(),
            episodeDao = database.episodeDao()
        )

        animeRepository = AnimeRepositoryImpl(sourceManager)
        sourceRepository = SourceRepositoryImpl(sourceManager)
        watchHistoryRepository = WatchHistoryRepositoryImpl(database.watchHistoryDao())
        favoriteRepository = FavoriteRepositoryImpl(database.favoriteDao(), database.animeDao())
    }
}
