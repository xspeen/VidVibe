package com.vidvibe.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
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
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.vidvibe.app.MainActivity
import com.vidvibe.app.R
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.ui.theme.AnimationStyle
import com.vidvibe.app.ui.theme.AppFont
import com.vidvibe.app.ui.theme.AppThemeMode
import com.vidvibe.app.ui.theme.BackgroundLook
import com.vidvibe.app.ui.theme.LocalAnimationDuration
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.LocalMenuPalette
import com.vidvibe.app.ui.theme.MenuGlass
import com.vidvibe.app.ui.theme.MenuPalette
import com.vidvibe.app.ui.theme.TextTone
import com.vidvibe.app.ui.theme.VidVibeTheme
import com.vidvibe.app.ui.theme.backgroundFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Floating "notch" navigation overlay.
 *
 * Runs as a special-use foreground service and draws a compact glass pill at the top
 * of the screen. Tapping the pill expands a panel with a paste-to-download shortcut
 * and quick navigation into every VidVibe tab.
 */
class NotchOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsStore by lazy { SettingsStore(this) }
    private val settings = MutableStateFlow(AppSettings())

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

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
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
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
            description = "Keeps the floating notch navigation running."
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
            .setContentText("Quick paste and navigation are ready")
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

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            ViewTreeLifecycleOwner.set(this, owner)
            ViewTreeSavedStateRegistryOwner.set(this, owner)
            ViewTreeViewModelStoreOwner.set(this, owner)
            setContent {
                val current by settings.collectAsState()
                OverlayTheme(current) {
                    NotchOverlay(
                        onOpenTab = { tab -> launchApp(tab = tab) },
                        onPaste = { launchApp(tab = 1, paste = true) },
                        onDisable = { disableNotch() }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = statusBarHeight() + dp(6)
        }

        overlayView = view
        return runCatching { windowManager.addView(view, params) }.isSuccess
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun launchApp(tab: Int? = null, paste: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            if (tab != null) putExtra(EXTRA_OPEN_TAB, tab)
            if (paste) putExtra(EXTRA_PASTE_LINK, true)
        }
        runCatching { startActivity(intent) }
    }

    private fun disableNotch() {
        serviceScope.launch {
            settingsStore.setNotchEnabled(false)
            stopSelf()
        }
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
    onOpenTab: (Int) -> Unit,
    onPaste: () -> Unit,
    onDisable: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val duration = LocalAnimationDuration.current.coerceIn(120, 900)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val panelWidth = (screenWidth - 28.dp).coerceAtMost(384.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NotchPill(
            expanded = expanded,
            onToggle = { expanded = !expanded }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(duration)) +
                expandVertically(tween(duration), expandFrom = Alignment.Top),
            exit = fadeOut(tween(duration)) +
                shrinkVertically(tween(duration), shrinkTowards = Alignment.Top)
        ) {
            NotchPanel(
                width = panelWidth,
                onOpenTab = { tab ->
                    expanded = false
                    onOpenTab(tab)
                },
                onPaste = {
                    expanded = false
                    onPaste()
                },
                onDisable = {
                    expanded = false
                    onDisable()
                }
            )
        }
    }
}

@Composable
private fun NotchPill(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val palette = LocalMenuPalette.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp
    )

    Row(
        modifier = Modifier
            .width(168.dp)
            .height(38.dp)
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
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color(0xFF30D158))
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(CircleShape)
                .background(Color(0xFF050507))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2A44))
            )
            Box(
                modifier = Modifier
                    .size(2.dp)
                    .clip(CircleShape)
                    .background(Color(0x665AA8FF))
                    .graphicsLayer {
                        translationX = -1.5f
                        translationY = -1.5f
                    }
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = Icons.Outlined.ChevronDown,
            contentDescription = if (expanded) "Collapse notch" else "Expand notch",
            tint = textColor.copy(alpha = 0.75f),
            modifier = Modifier
                .size(15.dp)
                .rotate(if (expanded) 180f else 0f)
        )
    }
}

@Composable
private fun NotchPanel(
    width: Dp,
    onOpenTab: (Int) -> Unit,
    onPaste: () -> Unit,
    onDisable: () -> Unit
) {
    val textColor = LocalAppTextColor.current
    val palette = LocalMenuPalette.current
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .width(width)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NOTCH NAVIGATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = textColor.copy(alpha = 0.55f),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(onClick = onDisable),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = "Turn off floating notch",
                    tint = textColor.copy(alpha = 0.70f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
                .clickable(onClick = onPaste)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x334E9BFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = Color(0xFF4E9BFF),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Paste link to download",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Opens Downloader with your clipboard",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.55f)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NotchNavItem(Icons.Outlined.VideoLibrary, "Local") { onOpenTab(0) }
            NotchNavItem(Icons.Outlined.Download, "Downloader") { onOpenTab(1) }
            NotchNavItem(Icons.Outlined.DeleteOutline, "Trash") { onOpenTab(2) }
            NotchNavItem(Icons.Outlined.PersonOutline, "Profile") { onOpenTab(3) }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tap the notch to collapse",
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.38f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NotchNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val textColor = LocalAppTextColor.current

    Column(
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor.copy(alpha = 0.80f)
        )
    }
}
