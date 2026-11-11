package com.vidvibe.app.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hosts the app's own navigation inside a movable, resizable overlay window so
 * it can be used while another app stays in the foreground.
 */
class FloatingWindowService : Service() {

    companion object {
        private const val CHANNEL_ID = "vidvibe_floating_window"
        private const val NOTIFICATION_ID = 4802
        private const val PREFS = "floating_window_state"

        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_COLLAPSED = "collapsed"

        fun hasPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

        fun start(context: Context) {
            if (!hasPermission(context)) return
            val intent = Intent(context, FloatingWindowService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, FloatingWindowService::class.java)) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    private lateinit var windowManager: WindowManager

    private val viewModelStore = ViewModelStore()
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(
            viewModelStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(application) as T
            }
        ).get(MainViewModel::class.java)
    }

    private var overlayView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null
    private var lifecycleOwner: FloatingWindowOwner? = null

    private var expandedWidthPx = 0
    private var expandedHeightPx = 0

    // Compose state: the window content swaps between the full card and the
    // compact bubble, so the flag has to be observable.
    private var collapsedState = mutableStateOf(false)
    private var collapsed: Boolean
        get() = collapsedState.value
        set(value) {
            collapsedState.value = value
        }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForegroundNotification()

        if (!showWindow()) {
            stopSelf()
            return
        }

        // Collect the raw store: the ViewModel's state flow starts with the
        // default settings, which would stop the service before the saved
        // preference has even loaded.
        val settingsStore = SettingsStore(this)
        serviceScope.launch {
            settingsStore.settings.collect { settings ->
                if (!settings.floatingWindowEnabled) stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        clampToScreen()
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        params = null
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        viewModelStore.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showWindow(): Boolean {
        if (!Settings.canDrawOverlays(this) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return false
        }

        val owner = FloatingWindowOwner()
        owner.onCreate()
        owner.onResume()
        lifecycleOwner = owner

        val screenWidth = screenSize().first
        val screenHeight = screenSize().second

        expandedWidthPx = prefs.getInt(KEY_WIDTH, defaultWidth()).coerceIn(minWidth(), screenWidth)
        expandedHeightPx =
            prefs.getInt(KEY_HEIGHT, defaultHeight()).coerceIn(minHeight(), screenHeight)
        collapsed = prefs.getBoolean(KEY_COLLAPSED, false)

        val startX = prefs.getInt(KEY_X, (screenWidth - expandedWidthPx) / 2)
        val startY = prefs.getInt(KEY_Y, dp(64))

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
                val settings by viewModel.settings.collectAsState()
                CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) {
                    FloatingTheme(settings) {
                        FloatingAppShell(
                            viewModel = viewModel,
                            settings = settings,
                            compact = collapsed,
                            onCollapse = { applyCollapsed(!collapsed) },
                            onClose = {
                                viewModel.setFloatingWindowEnabled(false)
                                stopSelf()
                            },
                            onDrag = { dx, dy -> moveBy(dx, dy) },
                            onDragEnd = { persist() },
                            onResize = { dx, dy -> resizeBy(dx, dy) },
                            onResizeEnd = { persist() },
                            onOpenInApp = { openInApp() },
                            onRequestOverlayPermission = { }
                        )
                    }
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            if (collapsed) collapsedSize() else expandedWidthPx,
            if (collapsed) collapsedSize() else expandedHeightPx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = startY
            softInputMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = view
        params = layoutParams
        val added = runCatching { windowManager.addView(view, layoutParams) }.isSuccess
        if (added) clampToScreen()
        return added
    }

    private fun applyCollapsed(value: Boolean) {
        val view = overlayView ?: return
        val layoutParams = params ?: return
        collapsed = value
        layoutParams.width = if (value) collapsedSize() else expandedWidthPx
        layoutParams.height = if (value) collapsedSize() else expandedHeightPx
        layoutParams.x = layoutParams.x.coerceIn(0, maxX())
        layoutParams.y = layoutParams.y.coerceIn(0, maxY())
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
        persist()
    }

    private fun moveBy(dx: Float, dy: Float) {
        val view = overlayView ?: return
        val layoutParams = params ?: return
        layoutParams.x = (layoutParams.x + dx.toInt()).coerceIn(0, maxX())
        layoutParams.y = (layoutParams.y + dy.toInt()).coerceIn(0, maxY())
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun resizeBy(dx: Float, dy: Float) {
        if (collapsed) return
        val view = overlayView ?: return
        val layoutParams = params ?: return
        expandedWidthPx = (expandedWidthPx + dx.toInt()).coerceIn(minWidth(), maxWidth())
        expandedHeightPx = (expandedHeightPx + dy.toInt()).coerceIn(minHeight(), maxHeight())
        layoutParams.width = expandedWidthPx
        layoutParams.height = expandedHeightPx
        layoutParams.x = layoutParams.x.coerceIn(0, maxX())
        layoutParams.y = layoutParams.y.coerceIn(0, maxY())
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun clampToScreen() {
        val view = overlayView ?: return
        val layoutParams = params ?: return
        if (!collapsed) {
            expandedWidthPx = expandedWidthPx.coerceIn(minWidth(), maxWidth())
            expandedHeightPx = expandedHeightPx.coerceIn(minHeight(), maxHeight())
            layoutParams.width = expandedWidthPx
            layoutParams.height = expandedHeightPx
        }
        layoutParams.x = layoutParams.x.coerceIn(0, maxX())
        layoutParams.y = layoutParams.y.coerceIn(0, maxY())
        runCatching { windowManager.updateViewLayout(view, layoutParams) }
    }

    private fun persist() {
        val layoutParams = params ?: return
        prefs.edit()
            .putInt(KEY_X, layoutParams.x)
            .putInt(KEY_Y, layoutParams.y)
            .putInt(KEY_WIDTH, expandedWidthPx)
            .putInt(KEY_HEIGHT, expandedHeightPx)
            .putBoolean(KEY_COLLAPSED, collapsed)
            .apply()
    }

    private fun openInApp() {
        // The full app is the right place for playback, so tuck the window away
        // instead of stacking two copies of the UI on top of each other.
        applyCollapsed(true)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            val point = android.graphics.Point()
            @Suppress("DEPRECATION")
            display.getSize(point)
            point.x to point.y
        }
    }

    private fun defaultWidth(): Int = (screenSize().first * 0.90f).toInt().coerceAtMost(dp(400))
    private fun defaultHeight(): Int = (screenSize().second * 0.66f).toInt().coerceAtMost(dp(620))

    private fun minWidth(): Int = dp(280)
    private fun minHeight(): Int = dp(360)
    private fun maxWidth(): Int = (screenSize().first - dp(8)).coerceAtLeast(minWidth())
    private fun maxHeight(): Int = (screenSize().second - dp(80)).coerceAtLeast(minHeight())
    private fun collapsedSize(): Int = dp(74)

    private fun maxX(): Int {
        val width = params?.width ?: collapsedSize()
        return (screenSize().first - width).coerceAtLeast(0)
    }

    private fun maxY(): Int {
        val height = params?.height ?: collapsedSize()
        return (screenSize().second - height - dp(48)).coerceAtLeast(0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating window",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the VidVibe floating window alive."
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun startForegroundNotification() {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_window)
            .setContentTitle("VidVibe floating window")
            .setContentText("Tap to open the app. Turn it off in Profile > App Settings.")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // specialUse only exists from Android 14; older versions use the
        // manifest type (none), so passing 0 is correct there.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        runCatching {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        }
    }
}

private class FloatingWindowOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner,
    OnBackPressedDispatcherOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val onBackPressedDispatcher = OnBackPressedDispatcher()

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

@Composable
private fun FloatingTheme(settings: AppSettings, content: @Composable () -> Unit) {
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
