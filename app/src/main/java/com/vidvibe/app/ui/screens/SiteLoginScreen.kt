package com.vidvibe.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vidvibe.app.download.CookieStore
import com.vidvibe.app.ui.theme.LocalAppBackground
import com.vidvibe.app.ui.theme.LocalAppTextColor

private data class LoginSite(val label: String, val url: String)

private val loginSites = listOf(
    LoginSite("Instagram", "https://www.instagram.com/"),
    LoginSite("TikTok", "https://www.tiktok.com/"),
    LoginSite("YouTube", "https://m.youtube.com/"),
    LoginSite("Facebook", "https://m.facebook.com/"),
    LoginSite("X", "https://x.com/"),
    LoginSite("Reddit", "https://www.reddit.com/"),
    LoginSite("Vimeo", "https://vimeo.com/"),
    LoginSite("Dailymotion", "https://www.dailymotion.com/")
)

/**
 * In-app sign-in window. Cookies never leave the device: once the user signs
 * in, [CookieStore] turns the WebView cookie jar into the cookies.txt that the
 * downloader already retries with, so login-only and age-gated posts work
 * without any manual export.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteLoginScreen(
    onClose: () -> Unit,
    onSaved: (Int) -> Unit
) {
    val context = LocalContext.current
    val background = LocalAppBackground.current
    val textColor = LocalAppTextColor.current

    var address by rememberSaveable { mutableStateOf("https://www.instagram.com/") }
    var currentUrl by rememberSaveable { mutableStateOf("https://www.instagram.com/") }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun normalize(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed.isBlank() -> "https://www.instagram.com/"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun navigate(value: String) {
        val target = normalize(value)
        currentUrl = target
        address = target
        error = null
        webView?.loadUrl(target)
    }

    fun save() {
        val target = webView?.url?.takeIf { it.startsWith("http") } ?: currentUrl
        val count = CookieStore.captureFromWebView(context, target)
        if (count > 0) {
            onSaved(count)
        } else {
            error = "Sign in on the site first, then tap Save sign-in."
        }
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onClose()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webView?.stopLoading()
                webView?.destroy()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Close sign-in",
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.10f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFF30D158),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(7.dp))
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 11.5.sp),
                    cursorBrush = SolidColor(Color(0xFF4E9BFF)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { navigate(address) })
                )
                if (loading) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4E9BFF))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { save() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            loginSites.forEach { site ->
                val selected = currentUrl.contains(
                    site.url.removePrefix("https://").removePrefix("www.").trimEnd('/')
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) Color.White.copy(alpha = 0.16f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (selected) Color.White.copy(alpha = 0.32f)
                            else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { navigate(site.url) }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = site.label,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = if (selected) 1f else 0.62f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            AndroidView(
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = settings.userAgentString +
                            " VidVibe/2.6"
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: android.graphics.Bitmap?
                            ) {
                                loading = true
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    address = url
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                if (!url.isNullOrBlank()) {
                                    currentUrl = url
                                    address = url
                                }
                            }
                        }
                        loadUrl(currentUrl)
                    }
                },
                update = { view ->
                    webView = view
                },
                modifier = Modifier.fillMaxSize()
            )

            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF4E9BFF).copy(alpha = 0.8f))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(15.dp)
                    .clickable { webView?.reload() }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = error ?: "Sign in once. VidVibe saves the session on this device and retries automatically.",
                fontSize = 10.sp,
                color = if (error != null) Color(0xFFFF8A8E) else textColor.copy(alpha = 0.42f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
