package com.vidvibe.app.browser

import android.content.Context

/**
 * Small, non-critical browser state that lives outside the app settings
 * DataStore so a browser preference never rewrites the user's theme.
 */
class BrowserPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("vidvibe_browser", Context.MODE_PRIVATE)

    var adBlock: Boolean
        get() = prefs.getBoolean("ad_block", true)
        set(value) = prefs.edit().putBoolean("ad_block", value).apply()

    var desktopMode: Boolean
        get() = prefs.getBoolean("desktop_mode", false)
        set(value) = prefs.edit().putBoolean("desktop_mode", value).apply()

    fun history(): List<String> =
        prefs.getString("history", "").orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

    fun addHistory(url: String) {
        if (!url.startsWith("http")) return
        val updated = (listOf(url) + history().filter { it != url }).take(30)
        prefs.edit().putString("history", updated.joinToString("\n")).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("history").apply()
    }
}
