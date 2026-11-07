package com.vidvibe.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vidVibeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vidvibe_settings"
)

data class AppSettings(
    val fontSizeSp: Float = 16f,
    val liquidGlass: Boolean = true,
    val backgroundColor: Int = 0xFF050507.toInt(),
    val wifiOnly: Boolean = false,
    val appLockEnabled: Boolean = false,
    val onboardingSeen: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val fontSize = floatPreferencesKey("font_size_sp")
        val liquidGlass = booleanPreferencesKey("liquid_glass")
        val backgroundColor = intPreferencesKey("background_color")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val appLock = booleanPreferencesKey("app_lock")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
    }

    val settings: Flow<AppSettings> = context.vidVibeDataStore.data.map { preferences ->
        AppSettings(
            fontSizeSp = preferences[Keys.fontSize] ?: 16f,
            liquidGlass = preferences[Keys.liquidGlass] ?: true,
            backgroundColor = preferences[Keys.backgroundColor] ?: 0xFF050507.toInt(),
            wifiOnly = preferences[Keys.wifiOnly] ?: false,
            appLockEnabled = preferences[Keys.appLock] ?: false,
            onboardingSeen = preferences[Keys.onboardingSeen] ?: false
        )
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.vidVibeDataStore.edit { it[Keys.fontSize] = sizeSp.coerceIn(12f, 24f) }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        context.vidVibeDataStore.edit { it[Keys.liquidGlass] = enabled }
    }

    suspend fun setBackgroundColor(color: Int) {
        context.vidVibeDataStore.edit { it[Keys.backgroundColor] = color }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.vidVibeDataStore.edit { it[Keys.wifiOnly] = enabled }
    }

    suspend fun setAppLock(enabled: Boolean) {
        context.vidVibeDataStore.edit { it[Keys.appLock] = enabled }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.vidVibeDataStore.edit { it[Keys.onboardingSeen] = seen }
    }
}
