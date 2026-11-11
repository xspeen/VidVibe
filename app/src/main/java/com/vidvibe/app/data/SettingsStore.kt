package com.vidvibe.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    val onboardingSeen: Boolean = false,
    val animationStyle: String = "bouncy",
    val appFont: String = "inter",
    val backgroundLook: String = "midnight",
    val textColor: String = "white",
    val glassColor: String = "default",
    val appTheme: String = "dark",
    val notchEnabled: Boolean = false
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val fontSize = floatPreferencesKey("font_size_sp")
        val liquidGlass = booleanPreferencesKey("liquid_glass")
        val backgroundColor = intPreferencesKey("background_color")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val appLock = booleanPreferencesKey("app_lock")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val animationStyle = stringPreferencesKey("animation_style")
        val appFont = stringPreferencesKey("app_font")
        val backgroundLook = stringPreferencesKey("background_look")
        val textColor = stringPreferencesKey("text_color")
        val glassColor = stringPreferencesKey("glass_color")
        val appTheme = stringPreferencesKey("app_theme")
        val notchEnabled = booleanPreferencesKey("notch_enabled")
    }

    val settings: Flow<AppSettings> = context.vidVibeDataStore.data.map { preferences ->
        AppSettings(
            fontSizeSp = preferences[Keys.fontSize] ?: 16f,
            liquidGlass = preferences[Keys.liquidGlass] ?: true,
            backgroundColor = preferences[Keys.backgroundColor] ?: 0xFF050507.toInt(),
            wifiOnly = preferences[Keys.wifiOnly] ?: false,
            appLockEnabled = preferences[Keys.appLock] ?: false,
            onboardingSeen = preferences[Keys.onboardingSeen] ?: false,
            animationStyle = preferences[Keys.animationStyle] ?: "bouncy",
            appFont = preferences[Keys.appFont] ?: "inter",
            backgroundLook = preferences[Keys.backgroundLook] ?: "midnight",
            textColor = preferences[Keys.textColor] ?: "white",
            glassColor = preferences[Keys.glassColor] ?: "default",
            appTheme = preferences[Keys.appTheme] ?: "dark",
            notchEnabled = preferences[Keys.notchEnabled] ?: false
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

    suspend fun setNotchEnabled(enabled: Boolean) {
        context.vidVibeDataStore.edit { it[Keys.notchEnabled] = enabled }
    }

    suspend fun setCustomization(
        animationStyle: String,
        appFont: String,
        backgroundLook: String,
        textColor: String,
        glassColor: String,
        appTheme: String
    ) {
        context.vidVibeDataStore.edit { preferences ->
            preferences[Keys.animationStyle] = animationStyle
            preferences[Keys.appFont] = appFont
            preferences[Keys.backgroundLook] = backgroundLook
            preferences[Keys.textColor] = textColor
            preferences[Keys.glassColor] = glassColor
            preferences[Keys.appTheme] = appTheme
        }
    }
}
