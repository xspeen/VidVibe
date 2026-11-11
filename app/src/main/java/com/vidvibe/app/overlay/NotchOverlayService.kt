package com.vidvibe.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vidvibe.app.MainActivity
import com.vidvibe.app.R
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.data.VideoRepository
import com.vidvibe.app.download.YtDlpManager
import com.vidvibe.app.ui.theme.AnimationStyle
import com.vidvibe.app.ui.theme.AppFont
import com.vidvibe.app.ui.theme.AppThemeMode
import com.vidvibe.app.ui.theme.BackgroundLook
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.LocalMenuPalette
import com.vidvibe.app.ui.theme.MenuGlass
import com.vidvibe.app.ui.theme.MenuPalette
import com.vidvibe.app.ui.theme.TextTone
import com.vidvibe.app.ui.theme.VidVibeTheme
import com.vidvibe.app.ui.theme.backgroundFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface NotchDownloadState {
    data object Idle : NotchDownloadState
    data class Running(val progress: Int, val status: String) : NotchDownloadState
    data class Complete(val fileName: String) : NotchDownloadState
    data class Error(val message: String) : NotchDownloadState
}

/**
 * Floating "notch" download overlay.
 *
 * Runs as a special-use foreground service and draws a compact glass pill that sits just
 * below the device's own camera cutout / status bar. Tapping the pill slides open an
 * in-notch download panel: paste a link, pick a quality, and the green progress bar fills
 * as the download runs. When it finishes the notch collapses itself.
 */
class NotchOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsStore by lazy { SettingsStore(this) }
    private val settings = MutableStateFlow(AppSettings())
    private val downloadState = MutableStateFlow<NotchDownloadState>(NotchDownloadState.Idle)
    private val repository by lazy { VideoRepository(this) }
    private val ytDlpManager by lazy { YtDlpManager(this) }

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var downloadJob: Job? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingWidthPx = 0
    private var pendingHeightPx = 0
    private val applyWindowSize = Runnable { commitWindowSize() }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundNotification()

        if (!showOverlay()) {
            stopSelf()
            return
        }

        serviceScope.launch {
            settingsStore.settings.collect { value ->
                settings.value = value
                if (!value.notchEnabled) stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadJob?.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        overlayParams = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating notch",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the floating notch download panel running."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notch)
            .setContentTitle("Floating notch is on")
            .setContentText("Paste a link in the notch to download")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun showOverlay(): Boolean {
        if (!Settings.canDrawOverlays(this)) return false

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onResume()
        lifecycleOwner = owner

        // Measure the real status bar / camera cutout so the overlay can be
        // anchored to the hardware notch instead of a guessed offset.
        val anchor = computeOverlayAnchor()

        val view = ComposeView(this).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                val current by settings.collectAsState()
                val download by downloadState.collectAsState()
                OverlayTheme(current) {
                    NotchOverlay(
                        download = download,
                        onContentSizeChanged = { width, height -> syncWindowSize(width, height) },
                        onExpandedChanged = { expanded -> setPanelFocus(expanded) },
                        onPasteClipboard = { readClipboard() },
                        onDownload = { url, quality -> startNotchDownload(url, quality) },
                        onCancel = { cancelNotchDownload() },
                        onReset = { resetNotchDownload() }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            overlayFlags(focusable = false),
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // FLAG_LAYOUT_NO_LIMITS lets these coordinates be measured from the
            // physical top of the display, so the window anchors directly to the
            // hardware notch instead of the usable content area.
            x = anchor.centerOffsetPx
            y = anchor.topPx

            // Without this the window is still kept clear of the camera cutout,
            // which pushes the notch UI back down below it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Do not inset the window for the status bar / navigation bar.
                fitInsetsTypes = 0
            }
        }

        overlayView = view
        overlayParams = params
        return runCatching { windowManager.addView(view, params) }.isSuccess
    }

    private fun overlayFlags(focusable: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (!focusable) {
            flags = flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        return flags
    }

    private fun setPanelFocus(focusable: Boolean) {
        val view = overlayView ?: return
        val params = overlayParams ?: return

        params.flags = overlayFlags(focusable)
        params.softInputMode = if (focusable) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        }

        if (focusable) {
            runCatching { windowManager.updateViewLayout(view, params) }
            runCatching { view.requestFocus() }
            relayoutDuringMorph()
        } else {
            // Drop the IME before the window loses focus, otherwise the
            // keyboard can stay up over the collapsed pill.
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            runCatching { imm?.hideSoftInputFromWindow(view.windowToken, 0) }
            runCatching { view.clearFocus() }
            runCatching { windowManager.updateViewLayout(view, params) }
            relayoutDuringMorph()
        }
    }

    /**
     * A WRAP_CONTENT overlay window re-measures through ViewRootImpl only, and
     * the input region of a LAYOUT_NO_LIMITS window is recomputed on a real
     * WindowManager relayout. Without that, the panel is drawn at its new size
     * while the touchable region is still the collapsed pill — taps inside the
     * panel then land on whatever is behind it. Forcing a relayout keeps the
     * hit area in sync with the visible glass.
     */
    private fun syncWindowSize(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        if (widthPx == pendingWidthPx && heightPx == pendingHeightPx) return
        pendingWidthPx = widthPx
        pendingHeightPx = heightPx
        scheduleRelayout()
    }

    private fun scheduleRelayout() {
        mainHandler.removeCallbacks(applyWindowSize)
        mainHandler.post(applyWindowSize)
    }

    private fun commitWindowSize() {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /** Re-applies the layout a few times while the panel morphs open. */
    private fun relayoutDuringMorph() {
        val offsets = longArrayOf(0L, 48L, 120L, 240L)
        offsets.forEach { delayMs ->
            mainHandler.postDelayed({ commitWindowSize() }, delayMs)
        }
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun readClipboard(): String {
        val manager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return manager
            ?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?.trim()
            .orEmpty()
    }

    private fun startNotchDownload(rawUrl: String, quality: Int) {
        if (downloadJob?.isActive == true) return

        val cleanUrl = rawUrl.trim()
        val parsed = runCatching { Uri.parse(cleanUrl) }.getOrNull()
        if (
            parsed == null ||
            parsed.scheme !in setOf("http", "https") ||
            parsed.host.isNullOrBlank()
        ) {
            downloadState.value =
                NotchDownloadState.Error("Paste a valid http or https video link.")
            return
        }

        if (settings.value.wifiOnly && !isOnWifi()) {
            downloadState.value = NotchDownloadState.Error(
                "Wi-Fi only is on. Connect to Wi-Fi or turn it off in settings."
            )
            return
        }

        downloadJob = serviceScope.launch {
            downloadState.value = NotchDownloadState.Running(0, "Preparing download...")
            try {
                val file = ytDlpManager.downloadVideo(
                    url = cleanUrl,
                    qualityHeight = quality
                ) { percent, status ->
                    downloadState.value = NotchDownloadState.Running(percent, status)
                }
                if (!isActive) return@launch

                val galleryName = if (file.name.startsWith("VidVibe_")) {
                    file.name
                } else {
                    "VidVibe_${file.name}"
                }
                val mimeType = repository.mimeTypeFor(galleryName)
                repository.saveToGallery(file, galleryName, mimeType)
                runCatching { file.delete() }
                if (!isActive) return@launch

                downloadState.value = NotchDownloadState.Complete(galleryName)
            } catch (error: Throwable) {
                if (error is CancellationException) return@launch
                val message = error.message
                    ?.substringBefore('\n')
                    ?.takeIf { it.isNotBlank() }
                    ?: "The download could not be completed."
                downloadState.value = NotchDownloadState.Error(message)
            }
        }
    }

    private fun cancelNotchDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloadState.value = NotchDownloadState.Idle
    }

    private fun resetNotchDownload() {
        if (downloadState.value !is NotchDownloadState.Running) {
            downloadState.value = NotchDownloadState.Idle
        }
    }

    private fun isOnWifi(): Boolean {
        val connectivity = getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager ?: return true
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private data class OverlayAnchor(
        val topPx: Int,
        val centerOffsetPx: Int
    )

    /**
     * Resolves the exact top inset and horizontal centre of the device's own
     * status bar / camera cutout. With a cutout the pill is centred on the
     * hardware notch; without one it sits just below the status bar.
     */
    @Suppress("DEPRECATION")
    private fun computeOverlayAnchor(): OverlayAnchor {
        val screenWidth = resources.displayMetrics.widthPixels
        var topInset = statusBarHeight()
        var cutoutTop = -1
        var cutoutBottom = -1
        var cutoutCenterX = screenWidth / 2

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()
            )
            if (insets.top > 0) topInset = insets.top

            val cutout = metrics.windowInsets.displayCutout
            val rect = cutout?.boundingRects?.firstOrNull {
                it.width() > 0 && it.height() > 0 && it.top < dp(80)
            }
            if (rect != null) {
                cutoutTop = rect.top
                cutoutBottom = rect.bottom
                cutoutCenterX = rect.centerX()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cutout = windowManager.defaultDisplay.cutout
            val rect = cutout?.boundingRects?.firstOrNull {
                it.width() > 0 && it.height() > 0 && it.top < dp(80)
            }
            if (rect != null) {
                cutoutTop = rect.top
                cutoutBottom = rect.bottom
                cutoutCenterX = rect.centerX()
            }
        }

        val topPx = if (cutoutTop >= 0 && cutoutBottom > cutoutTop) {
            // Align the pill's vertical centre with the camera cutout so it
            // hugs the hardware notch (Dynamic Island style).
            ((cutoutTop + cutoutBottom) / 2 - dp(20)).coerceAtLeast(0)
        } else {
            // No cutout on this device: keep the pill below the status bar.
            topInset + dp(4)
        }
        // Keep the whole notch UI on-screen even when the cutout is off-centre.
        val panelWidthPx = (screenWidth - dp(24))
            .coerceAtMost(dp(400))
            .coerceAtLeast(0)
        val maxShift = ((screenWidth - panelWidthPx) / 2).coerceAtLeast(0)
        val centerOffsetPx = if (cutoutTop >= 0) {
            (cutoutCenterX - screenWidth / 2).coerceIn(-maxShift, maxShift)
        } else {
            0
        }
        return OverlayAnchor(topPx, centerOffsetPx)
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        } else {
            (24 * resources.displayMetrics.density).toInt()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        SavedStateRegistryOwner,
        ViewModelStoreOwner {

        private val registry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun onCreate() {
            savedStateController.performRestore(null)
            registry.currentState = Lifecycle.State.CREATED
        }

        fun onResume() {
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun onDestroy() {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "com.vidvibe.app.extra.OPEN_TAB"
        const val EXTRA_PASTE_LINK = "com.vidvibe.app.extra.PASTE_LINK"

        private const val CHANNEL_ID = "vidvibe_notch"
        private const val NOTIFICATION_ID = 4102

        fun hasPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

        fun start(context: Context) {
            if (!hasPermission(context)) return
            val intent = Intent(context, NotchOverlayService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, NotchOverlayService::class.java)) }
        }
    }
}

@Composable
private fun OverlayTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val animation = AnimationStyle.from(settings.animationStyle)
    val font = AppFont.from(settings.appFont)
    val look = BackgroundLook.from(settings.backgroundLook)
    val tone = TextTone.from(settings.textColor)
    val glass = MenuGlass.from(settings.glassColor)
    val theme = AppThemeMode.from(settings.appTheme)

    VidVibeTheme(
        background = backgroundFor(look, theme),
        fontScaleSp = settings.fontSizeSp,
        fontFamily = font.family,
        textColor = tone.color,
        animationDurationMs = animation.durationMs,
        menuPalette = MenuPalette(glass.background, glass.highlight),
        content = content
    )
}

@Composable
private fun NotchOverlay(
    download: NotchDownloadState,
    onContentSizeChanged: (Int, Int) -> Unit,
    onExpandedChanged: (Boolean) -> Unit,
    onPasteClipboard: () -> String,
    onDownload: (String, Int) -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var link by remember { mutableStateOf("") }
    var quality by remember { mutableIntStateOf(720) }
    var pasteHint by remember { mutableStateOf(false) }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth = (screenWidth - 24.dp).coerceAtMost(400.dp)

    // Dynamic Island feel: the pill itself grows into the panel instead of a
    // separate card sliding out underneath it. Snappy spring, slight overshoot.
    val morph = spring<Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow * 4f
    )
    val pillWidth by animateDpAsState(
        targetValue = if (expanded) panelWidth else 172.dp,
        animationSpec = morph,
        label = "notch-width"
    )
    val pillHeight by animateDpAsState(
        targetValue = if (expanded) 44.dp else 40.dp,
        animationSpec = morph,
        label = "notch-height"
    )
    val pillBottomRadius by animateDpAsState(
        targetValue = if (expanded) 10.dp else 26.dp,
        animationSpec = morph,
        label = "notch-radius"
    )
    val gap by animateDpAsState(
        targetValue = if (expanded) 0.dp else 8.dp,
        animationSpec = morph,
        label = "notch-gap"
    )

    LaunchedEffect(expanded) {
        onExpandedChanged(expanded)
        if (expanded) {
            // The window only gains focus once it becomes focusable, so give it a
            // moment before touching the clipboard. Retry once if the system
            // was not ready yet on the first attempt.
            delay(90)
            if (link.isBlank()) {
                val pasted = onPasteClipboard()
                if (pasted.isNotBlank()) {
                    link = pasted
                } else {
                    delay(220)
                    if (link.isBlank()) {
                        val retry = onPasteClipboard()
                        if (retry.isNotBlank()) link = retry
                    }
                }
            }
        }
    }

    LaunchedEffect(pasteHint) {
        if (pasteHint) {
            delay(2600)
            pasteHint = false
        }
    }

    LaunchedEffect(download) {
        if (download is NotchDownloadState.Complete) {
            delay(1400)
            expanded = false
            onReset()
        }
    }

    Column(
        modifier = Modifier.onSizeChanged { size ->
            onContentSizeChanged(size.width, size.height)
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NotchPill(
            expanded = expanded,
            width = pillWidth,
            height = pillHeight,
            bottomRadius = pillBottomRadius,
            download = download,
            onToggle = { expanded = !expanded }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(110)) +
                expandVertically(
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 1400f),
                    expandFrom = Alignment.Top
                ),
            exit = fadeOut(tween(80)) +
                shrinkVertically(tween(130), shrinkTowards = Alignment.Top)
        ) {
            NotchPanel(
                width = panelWidth,
                gap = gap,
                link = link,
                onLinkChange = { link = it },
                quality = quality,
                onQualityChange = { quality = it },
                download = download,
                onPaste = {
                    val pasted = onPasteClipboard()
                    if (pasted.isNotBlank()) {
                        link = pasted
                        pasteHint = false
                    } else {
                        // Android 10+ hides the clipboard from windows that do
                        // not hold input focus yet; the field's own long-press
                        // paste always works, so point at it instead of doing
                        // nothing at all.
                        pasteHint = true
                    }
                },
                pasteHint = pasteHint,
                onDownload = { onDownload(link, quality) },
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun NotchPill(
    expanded: Boolean,
    width: Dp,
    height: Dp,
    bottomRadius: Dp,
    download: NotchDownloadState,
    onToggle: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val palette = LocalMenuPalette.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.955f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 2400f),
        label = "notch-press"
    )

    val dotColor = when (download) {
        is NotchDownloadState.Running -> Color(0xFF4E9BFF)
        is NotchDownloadState.Complete -> Color(0xFF30D158)
        is NotchDownloadState.Error -> Color(0xFFFF6B6B)
        NotchDownloadState.Idle -> Color(0xFF30D158)
    }
    val label = when (download) {
        is NotchDownloadState.Running -> "${download.progress}%"
        is NotchDownloadState.Complete -> "Saved"
        is NotchDownloadState.Error -> "Retry"
        NotchDownloadState.Idle -> "Download"
    }

    Row(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xF20A0A0C),
                        palette.background.copy(alpha = 0.96f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                // Light tick only — the old LongPress buzz made taps feel heavy.
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.85f)
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
            else Icons.Outlined.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse download notch"
            else "Open download notch",
            tint = textColor.copy(alpha = 0.75f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun NotchPanel(
    width: Dp,
    gap: Dp,
    link: String,
    onLinkChange: (String) -> Unit,
    quality: Int,
    onQualityChange: (Int) -> Unit,
    download: NotchDownloadState,
    pasteHint: Boolean,
    onPaste: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val palette = LocalMenuPalette.current
    // Top corners stay square so the panel reads as one piece with the pill,
    // the way the hardware Dynamic Island unfolds.
    val shape = RoundedCornerShape(
        topStart = 10.dp,
        topEnd = 10.dp,
        bottomStart = 26.dp,
        bottomEnd = 26.dp
    )
    val running = download is NotchDownloadState.Running

    // Unfold from the pill's width so the window never becomes wider than the
    // visible glass — a wide, half-empty window would swallow taps in its dead
    // space while the panel animates in.
    val unfold = spring<Dp>(dampingRatio = 0.86f, stiffness = 1600f)
    var unfolded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { unfolded = true }
    val animatedWidth by animateDpAsState(
        targetValue = if (unfolded) width else 172.dp,
        animationSpec = unfold,
        label = "notch-panel-width"
    )

    Column(
        modifier = Modifier
            .padding(top = gap)
            .width(animatedWidth)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        palette.background.copy(alpha = 0.97f),
                        Color(0xF2050507)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = Color(0xFF4E9BFF),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = link,
                onValueChange = onLinkChange,
                singleLine = true,
                enabled = !running,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 13.sp
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                cursorBrush = SolidColor(Color(0xFF30D158)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (link.isEmpty()) {
                            Text(
                                text = if (pasteHint) "Long-press to paste"
                                else "Paste a video link",
                                fontSize = 13.sp,
                                color = if (pasteHint) Color(0xFF9CC7FF)
                                else textColor.copy(alpha = 0.40f)
                            )
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0x334E9BFF))
                    .clickable(enabled = !running, onClick = onPaste)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "Paste",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF9CC7FF)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QualityChip("4K", 2160, quality, onQualityChange, running)
            QualityChip("1080p", 1080, quality, onQualityChange, running)
            QualityChip("720p", 720, quality, onQualityChange, running)
            QualityChip("480p", 480, quality, onQualityChange, running)
            QualityChip("360p", 360, quality, onQualityChange, running)
        }

        Spacer(Modifier.height(12.dp))

        when (download) {
            is NotchDownloadState.Running -> {
                val fraction by animateFloatAsState(
                    targetValue = (download.progress / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(300),
                    label = "notch-progress"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${download.progress}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF30D158)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Cancel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.65f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1FA84A), Color(0xFF30D158))
                                )
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = download.status,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.55f),
                    maxLines = 1
                )
            }

            is NotchDownloadState.Complete -> {
                Text(
                    text = "Saved to gallery",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF30D158),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = download.fileName,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            else -> {
                val error = download as? NotchDownloadState.Error
                if (error != null) {
                    Text(
                        text = error.message,
                        fontSize = 11.sp,
                        color = Color(0xFFFF8080),
                        lineHeight = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
                DownloadButton(
                    enabled = link.isNotBlank(),
                    onClick = onDownload
                )
            }
        }
    }
}

@Composable
private fun RowScope.QualityChip(
    label: String,
    value: Int,
    selected: Int,
    onSelect: (Int) -> Unit,
    disabled: Boolean
) {
    val textColor = LocalAppTextColor.current
    val active = value == selected
    Box(
        modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (active) Color(0x3330D158) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (active) Color(0x6630D158) else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(9.dp)
            )
            .clickable(enabled = !disabled) { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) Color(0xFF30D158) else textColor.copy(alpha = 0.70f)
        )
    }
}

@Composable
private fun DownloadButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 2600f),
        label = "download-press"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF1FA84A), Color(0xFF30D158))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color(0x331FA84A), Color(0x3330D158))
                    )
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Download,
            contentDescription = null,
            tint = if (enabled) Color(0xFF04140A) else Color(0xFF6E8F78),
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "Download",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color(0xFF04140A) else Color(0xFF6E8F78)
        )
    }
}
