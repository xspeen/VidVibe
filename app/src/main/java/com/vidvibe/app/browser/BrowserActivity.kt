package com.vidvibe.app.browser

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.ui.theme.AnimationStyle
import com.vidvibe.app.ui.theme.AppFont
import com.vidvibe.app.ui.theme.AppThemeMode
import com.vidvibe.app.ui.theme.BackgroundLook
import com.vidvibe.app.ui.theme.GlassStyle
import com.vidvibe.app.ui.theme.IconPack
import com.vidvibe.app.ui.theme.MenuGlass
import com.vidvibe.app.ui.theme.MenuPalette
import com.vidvibe.app.ui.theme.TextTone
import com.vidvibe.app.ui.theme.VidVibeTheme
import com.vidvibe.app.ui.theme.backgroundFor

/**
 * Hosts the full VidVibe browser. It is also a real VIEW handler for http and
 * https links, so it can be chosen as the device browser.
 */
class BrowserActivity : FragmentActivity() {

    private val store by lazy { SettingsStore(this) }
    private val incomingUrl = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        incomingUrl.value = intent?.dataString

        setContent {
            val settings by store.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            val glass = MenuGlass.from(settings.glassColor)
            val palette = MenuPalette(glass.background, glass.highlight)

            VidVibeTheme(
                background = backgroundFor(
                    BackgroundLook.from(settings.backgroundLook),
                    AppThemeMode.from(settings.appTheme)
                ),
                fontScaleSp = settings.fontSizeSp,
                fontFamily = AppFont.from(settings.appFont).family,
                textColor = TextTone.from(settings.textColor).color,
                animationDurationMs = AnimationStyle.from(settings.animationStyle).durationMs,
                menuPalette = palette,
                glassStyle = GlassStyle.from(palette, settings.liquidGlass),
                iconPack = IconPack.from(settings.iconPack)
            ) {
                BrowserScreen(
                    initialUrl = incomingUrl.value,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUrl.value = intent.dataString
    }
}
