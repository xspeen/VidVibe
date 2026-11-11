package com.vidvibe.app.share

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.ui.theme.AnimationStyle
import com.vidvibe.app.ui.theme.AppFont
import com.vidvibe.app.ui.theme.AppThemeMode
import com.vidvibe.app.ui.theme.BackgroundLook
import com.vidvibe.app.ui.theme.MenuGlass
import com.vidvibe.app.ui.theme.MenuPalette
import com.vidvibe.app.ui.theme.TextTone
import com.vidvibe.app.ui.theme.VidVibeTheme
import com.vidvibe.app.ui.theme.backgroundFor
import com.vidvibe.app.ui.viewmodel.MainViewModel
import android.graphics.Color as AndroidColor

/**
 * Share target. Any app that shares a link (Instagram, TikTok, YouTube,
 * Facebook, X, a browser) shows "Save with VidVibe". Picking it opens a
 * translucent drawer over the sending app instead of the full VidVibe UI.
 */
class ShareActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var sharedLink by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.TRANSPARENT))
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        sharedLink = extractLink(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

            val glass = MenuGlass.from(settings.glassColor)
            val appBackground = backgroundFor(
                BackgroundLook.from(settings.backgroundLook),
                AppThemeMode.from(settings.appTheme)
            )

            VidVibeTheme(
                background = appBackground,
                fontScaleSp = settings.fontSizeSp,
                fontFamily = AppFont.from(settings.appFont).family,
                textColor = TextTone.from(settings.textColor).color,
                animationDurationMs = AnimationStyle.from(settings.animationStyle).durationMs,
                menuPalette = MenuPalette(glass.background, glass.highlight)
            ) {
                ShareSheet(
                    initialLink = sharedLink,
                    state = downloadState,
                    onDownload = { link, height -> viewModel.startDownload(link, height) },
                    onCancel = {
                        viewModel.resetDownload()
                        finish()
                    },
                    onSaved = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sharedLink = extractLink(intent)
    }

    private fun extractLink(intent: Intent?): String {
        if (intent == null) return ""
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val combined = text.ifBlank { subject }
        val match = Regex("""https?://[^\s"'<>]+""").find(combined)?.value
        return (match ?: combined).trim().trimEnd('.', ',', ')', ']', '"', '\'')
    }
}
