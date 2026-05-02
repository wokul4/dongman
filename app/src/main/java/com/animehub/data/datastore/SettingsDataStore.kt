package com.animehub.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "animehub_settings")

class SettingsDataStore(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val ordinal = prefs[THEME_MODE_KEY] ?: ThemeMode.SYSTEM.ordinal
        ThemeMode.entries.getOrElse(ordinal) { ThemeMode.SYSTEM }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.ordinal
        }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
