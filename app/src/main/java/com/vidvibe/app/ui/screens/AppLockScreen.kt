package com.vidvibe.app.ui.screens

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vidvibe.app.ui.theme.VidVibeRed
import com.vidvibe.app.ui.theme.VidVibeSurface2
import com.vidvibe.app.ui.theme.VidVibeText
import com.vidvibe.app.ui.theme.VidVibeTextSecondary
import com.vidvibe.app.ui.theme.VidVibeTextTertiary

@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    var authenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var biometricsAvailable by remember { mutableStateOf(false) }
    var deviceCredentialAvailable by remember {
        mutableStateOf(
            (context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.isDeviceSecure == true
        )
    }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authenticating = false
        if (result.resultCode == Activity.RESULT_OK) {
            currentOnUnlocked()
        } else {
            errorMessage = "Unlock to open VidVibe"
        }
    }

    val pulse = rememberInfiniteTransition(label = "lock-pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock-pulse-scale"
    )

    fun authenticateWithDeviceCredential() {
        val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE)
            as? KeyguardManager
        val intent = keyguard?.createConfirmDeviceCredentialIntent(
            "Unlock VidVibe",
            "Confirm your device PIN, pattern, or password"
        )
        if (intent == null) {
            errorMessage = "Set a screen lock in Android settings to use app lock."
            return
        }
        authenticating = true
        errorMessage = null
        credentialLauncher.launch(intent)
    }

    fun authenticate() {
        val host = activity
        if (host == null) {
            errorMessage = "App lock needs a screen with biometric support."
            return
        }
        if (!biometricsAvailable) {
            if (deviceCredentialAvailable) {
                authenticateWithDeviceCredential()
            } else {
                errorMessage = "Set a screen lock in Android settings to use app lock."
            }
            return
        }
        authenticating = true
        errorMessage = null
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticating = false
                    currentOnUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    authenticating = false
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        deviceCredentialAvailable
                    ) {
                        authenticateWithDeviceCredential()
                        return
                    }
                    errorMessage = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED -> "Unlock to open VidVibe"
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                            "Too many attempts. Use your device PIN or wait."
                        BiometricPrompt.ERROR_NO_BIOMETRICS ->
                            "No biometrics enrolled. Use your device PIN."
                        else -> errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    errorMessage = "Not recognized. Try again."
                }
            }
        )
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock VidVibe")
            .setSubtitle("Use your fingerprint, face, or device screen lock")
            .setAllowedAuthenticators(allowed)
            .setConfirmationRequired(false)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            infoBuilder.setNegativeButtonText("Use device PIN")
        }
        runCatching { prompt.authenticate(infoBuilder.build()) }
            .onFailure {
                authenticating = false
                errorMessage = it.message ?: "Biometric unlock is not available."
            }
    }

    LaunchedEffect(Unit) {
        val manager = activity?.let { BiometricManager.from(it) }
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        biometricsAvailable = manager?.canAuthenticate(allowed) ==
            BiometricManager.BIOMETRIC_SUCCESS
        deviceCredentialAvailable =
            (context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.isDeviceSecure == true
        authenticate()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF08080B), Color(0xFF11121A), Color(0xFF08080B))
                )
            )
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(scale)
                    .background(VidVibeSurface2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    VidVibeRed.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            Text(
                text = "VidVibe is locked",
                style = MaterialTheme.typography.headlineSmall,
                color = VidVibeText,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Unlock with your fingerprint, face, or device screen lock to open your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = VidVibeTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = { authenticate() },
                enabled = !authenticating,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VidVibeRed,
                    contentColor = Color.White,
                    disabledContainerColor = VidVibeSurface2,
                    disabledContentColor = VidVibeTextTertiary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = if (authenticating) "Waiting for unlock..." else "Unlock VidVibe",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (deviceCredentialAvailable) {
                Spacer(Modifier.height(6.dp))
                TextButton(
                    onClick = { authenticateWithDeviceCredential() },
                    enabled = !authenticating
                ) {
                    Text(
                        text = "Use device PIN, pattern, or password",
                        style = MaterialTheme.typography.labelMedium,
                        color = VidVibeTextSecondary
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = errorMessage ?: if (biometricsAvailable || deviceCredentialAvailable) {
                    "Your biometric data never leaves your device."
                } else {
                    "Set a screen lock in Android settings to use app lock."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = if (errorMessage != null) VidVibeRed else VidVibeTextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}
