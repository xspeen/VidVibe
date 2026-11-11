package com.vidvibe.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vidvibe.app.ui.VidVibeApp
import com.vidvibe.app.ui.components.PermissionScreen
import com.vidvibe.app.ui.screens.AppLockScreen
import com.vidvibe.app.ui.screens.OnboardingScreen
import com.vidvibe.app.floating.FloatingWindowService
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

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prevent screenshots, screen recording, and the recent-apps thumbnail.
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
            var permissionGranted by remember {
                mutableStateOf(hasMediaPermission())
            }
            var permissionRequested by rememberSaveable { mutableStateOf(false) }
            var onboardingComplete by rememberSaveable { mutableStateOf(false) }
            var hasUnlocked by rememberSaveable { mutableStateOf(false) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                permissionGranted = hasMediaPermission()
            }

            val requestPermission = {
                permissionRequested = true
                permissionLauncher.launch(requiredMediaPermissions())
            }

            val lifecycleOwner = LocalLifecycleOwner.current
            val currentCheck by rememberUpdatedState(::hasMediaPermission)
            val currentLockEnabled by rememberUpdatedState(settings.appLockEnabled)
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> permissionGranted = currentCheck()
                        Lifecycle.Event.ON_STOP -> {
                            if (currentLockEnabled) hasUnlocked = false
                        }
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            val showOnboarding = settingsLoaded &&
                !settings.onboardingSeen &&
                !onboardingComplete
            val lockRequired = settingsLoaded &&
                settings.appLockEnabled &&
                !hasUnlocked
            val appReady = settingsLoaded && !showOnboarding && !lockRequired

            LaunchedEffect(settingsLoaded, settings.appLockEnabled) {
                if (settingsLoaded && !settings.appLockEnabled) {
                    hasUnlocked = true
                }
            }

            LaunchedEffect(appReady) {
                if (appReady && !permissionGranted && !permissionRequested) {
                    requestPermission()
                }
            }

            LaunchedEffect(settingsLoaded, settings.floatingWindowEnabled) {
                if (!settingsLoaded) return@LaunchedEffect
                if (settings.floatingWindowEnabled && FloatingWindowService.hasPermission(this@MainActivity)) {
                    FloatingWindowService.start(this@MainActivity)
                } else {
                    FloatingWindowService.stop(this@MainActivity)
                }
            }

            val animationStyle = AnimationStyle.from(settings.animationStyle)
            val appFont = AppFont.from(settings.appFont)
            val backgroundLook = BackgroundLook.from(settings.backgroundLook)
            val textTone = TextTone.from(settings.textColor)
            val menuGlass = MenuGlass.from(settings.glassColor)
            val appTheme = AppThemeMode.from(settings.appTheme)
            val appBackground = backgroundFor(backgroundLook, appTheme)

            VidVibeTheme(
                background = appBackground,
                fontScaleSp = settings.fontSizeSp,
                fontFamily = appFont.family,
                textColor = textTone.color,
                animationDurationMs = animationStyle.durationMs,
                menuPalette = MenuPalette(menuGlass.background, menuGlass.highlight)
            ) {
                Box(Modifier.fillMaxSize().background(appBackground)) {
                    when {
                        !settingsLoaded -> Unit
                        showOnboarding -> OnboardingScreen(
                            onFinish = {
                                onboardingComplete = true
                                viewModel.setOnboardingSeen(true)
                            }
                        )
                        lockRequired -> AppLockScreen(
                            onUnlocked = { hasUnlocked = true }
                        )
                        !permissionGranted -> PermissionScreen(
                            onAllow = requestPermission,
                            onOpenSettings = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", packageName, null)
                                )
                                startActivity(intent)
                            }
                        )
                        else -> VidVibeApp(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun requiredMediaPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    private fun hasMediaPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}
