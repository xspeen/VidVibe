package com.vidvibe.app.browser

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vidvibe.app.ui.theme.GlassStyle
import com.vidvibe.app.ui.theme.LocalAppTextColor
import com.vidvibe.app.ui.theme.LocalGlassStyle
import kotlinx.coroutines.delay

private val BrowserBlue = Color(0xFF4E9BFF)
private val BrowserGreen = Color(0xFF30D158)
private val BrowserRed = Color(0xFFEA4335)

private data class QuickLink(val label: String, val url: String, val color: Color)

private val quickLinks = listOf(
    QuickLink("YouTube", "https://m.youtube.com/", Color(0xFFFF4444)),
    QuickLink("Google", "https://www.google.com/", Color(0xFF4285F4)),
    QuickLink("Instagram", "https://www.instagram.com/", Color(0xFFE1306C)),
    QuickLink("TikTok", "https://www.tiktok.com/", Color(0xFF25F4EE)),
    QuickLink("X", "https://x.com/", Color(0xFFE7E9EA)),
    QuickLink("Reddit", "https://www.reddit.com/", Color(0xFFFF5700)),
    QuickLink("Facebook", "https://m.facebook.com/", Color(0xFF1877F2)),
    QuickLink("Vimeo", "https://vimeo.com/", Color(0xFF1AB7EA)),
    QuickLink("Dailymotion", "https://www.dailymotion.com/", Color(0xFF00AAFF)),
    QuickLink("Wikipedia", "https://www.wikipedia.org/", Color(0xFFB8B8B8))
)

/**
 * Full in-app browser: real tabs, an omnibox, a Chromium engine, request-level
 * ad blocking and automatic cookie capture into the downloader's session file.
 * The existing sign-in window is untouched; this is the general-purpose
 * browser behind the globe entry points.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserScreen(
    initialUrl: String?,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { BrowserPreferences(context) }
    val controller = remember {
        BrowserController(
            context = context,
            prefs = prefs,
            adBlockEnabled = prefs.adBlock,
            desktopEnabled = prefs.desktopMode
        )
    }
    val ui by controller.state
    val textColor = LocalAppTextColor.current
    val glass = LocalGlassStyle.current

    var address by rememberSaveable { mutableStateOf("") }
    var showTabs by rememberSaveable { mutableStateOf(false) }
    var showMenu by rememberSaveable { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var menuMessage by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf(prefs.history()) }
    var pendingFile by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingFile?.onReceiveValue(
            if (uris.isNullOrEmpty()) null else uris.toTypedArray()
        )
        pendingFile = null
    }

    DisposableEffect(Unit) {
        controller.onFileChooser = { callback ->
            pendingFile = callback
            fileLauncher.launch("*/*")
        }
        controller.onToast = { toast = it }
        onDispose { controller.destroy() }
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            controller.load(initialUrl)
            address = initialUrl
        }
    }

    LaunchedEffect(ui.activeIndex, ui.active?.url, ui.home) {
        address = if (ui.home) "" else ui.active?.url.orEmpty()
    }

    LaunchedEffect(ui.home) {
        if (ui.home) history = prefs.history()
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }

    BackHandler {
        when {
            showTabs -> showTabs = false
            showMenu -> showMenu = false
            !ui.home && controller.canGoBack() -> controller.goBack()
            !ui.home -> controller.goHome()
            else -> onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
    ) {
        Column(Modifier.fillMaxSize()) {
            BrowserTopBar(
                address = address,
                onAddressChange = { address = it },
                onSubmit = {
                    if (address.isNotBlank()) controller.load(address)
                    keyboard?.hide()
                },
                loading = ui.active?.loading == true,
                progress = ui.active?.progress ?: 0,
                secure = address.startsWith("https://"),
                tabCount = ui.tabs.size,
                onStop = { controller.stopLoading() },
                onReload = { controller.reload() },
                onHome = { controller.goHome() },
                onTabs = { showTabs = true },
                onMenu = {
                    menuMessage = null
                    history = prefs.history()
                    showMenu = true
                },
                glass = glass,
                textColor = textColor
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { controller.host },
                    modifier = Modifier.fillMaxSize()
                )

                if (ui.home) {
                    BrowserHome(
                        history = history,
                        adBlock = ui.adBlock,
                        onSearch = { controller.load(it) },
                        onOpen = { controller.load(it) },
                        onToggleAdBlock = { controller.setAdBlock(it) },
                        onClearHistory = {
                            prefs.clearHistory()
                            history = emptyList()
                        },
                        onMenu = {
                            menuMessage = null
                            showMenu = true
                        },
                        glass = glass,
                        textColor = textColor
                    )
                }

                ui.active?.error?.let { message ->
                    if (!ui.home) {
                        BrowserError(
                            message = message,
                            onRetry = { controller.reload() },
                            textColor = textColor
                        )
                    }
                }
            }

            BrowserBottomBar(
                canGoBack = ui.active?.canGoBack == true,
                canGoForward = ui.active?.canGoForward == true,
                tabCount = ui.tabs.size,
                onBack = { controller.goBack() },
                onForward = { controller.goForward() },
                onHome = { controller.goHome() },
                onTabs = { showTabs = true },
                onMenu = {
                    menuMessage = null
                    showMenu = true
                },
                glass = glass,
                textColor = textColor
            )
        }

        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 78.dp, start = 24.dp, end = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xF01A1C22))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = toast.orEmpty(),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        if (showTabs) {
            TabsOverlay(
                ui = ui,
                onSelect = { index ->
                    controller.selectTab(index)
                    showTabs = false
                },
                onClose = { index -> controller.closeTab(index) },
                onNew = {
                    controller.newTab(null)
                    showTabs = false
                },
                onDismiss = { showTabs = false },
                glass = glass,
                textColor = textColor
            )
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xFF111318),
            contentColor = textColor,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        ) {
            BrowserMenu(
                ui = ui,
                message = menuMessage,
                onNewTab = {
                    controller.newTab(null)
                    showMenu = false
                },
                onTabs = {
                    showMenu = false
                    showTabs = true
                },
                onToggleDesktop = { controller.setDesktop(it) },
                onToggleAdBlock = { controller.setAdBlock(it) },
                onSyncCookies = {
                    val count = controller.syncCookies()
                    menuMessage = if (count > 0) {
                        "Saved $count session cookies for the downloader."
                    } else {
                        "No cookies on this page yet. Sign in first."
                    }
                },
                onShare = {
                    val url = controller.currentUrl()
                    if (url.isNotBlank()) {
                        val send = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, url)
                        context.startActivity(Intent.createChooser(send, "Share page"))
                    }
                    showMenu = false
                },
                onExternal = {
                    val url = controller.currentUrl()
                    if (url.isNotBlank()) {
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                    "Open with"
                                )
                            )
                        }
                    }
                    showMenu = false
                },
                onClearData = {
                    controller.clearBrowsingData()
                    history = prefs.history()
                    menuMessage = "Browsing data cleared."
                },
                onClose = {
                    showMenu = false
                    onClose()
                },
                textColor = textColor
            )
        }
    }
}

@Composable
private fun BrowserTopBar(
    address: String,
    onAddressChange: (String) -> Unit,
    onSubmit: () -> Unit,
    loading: Boolean,
    progress: Int,
    secure: Boolean,
    tabCount: Int,
    onStop: () -> Unit,
    onReload: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    glass: GlassStyle,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0B10))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onHome),
                contentAlignment = Alignment.Center
            ) {
                BrowserLogo(24.dp)
            }

            Spacer(Modifier.width(6.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(glass.bodyTop)
                    .border(1.dp, glass.stroke, RoundedCornerShape(20.dp))
                    .padding(start = 11.dp, end = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (secure) Icons.Outlined.Lock else Icons.Outlined.Language,
                    contentDescription = null,
                    tint = if (secure) BrowserGreen else textColor.copy(alpha = 0.42f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(7.dp))
                BasicTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = textColor, fontSize = 12.5.sp),
                    cursorBrush = SolidColor(BrowserBlue),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    decorationBox = { inner ->
                        Box {
                            if (address.isBlank()) {
                                Text(
                                    text = "Search or enter address",
                                    fontSize = 12.5.sp,
                                    color = textColor.copy(alpha = 0.35f),
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    }
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { if (loading) onStop() else onReload() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (loading) Icons.Outlined.Close else Icons.Outlined.Refresh,
                        contentDescription = if (loading) "Stop" else "Reload",
                        tint = textColor.copy(alpha = 0.72f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(glass.bodyTop)
                    .border(1.dp, glass.stroke, RoundedCornerShape(9.dp))
                    .clickable(onClick = onTabs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabCount.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onMenu),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Browser menu",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((progress.coerceIn(0, 100)) / 100f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(BrowserBlue, BrowserGreen)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    glass: GlassStyle,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0B10))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(glass.stroke)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomAction(
                icon = Icons.Outlined.ArrowBack,
                label = "Back",
                enabled = canGoBack,
                onClick = onBack,
                textColor = textColor
            )
            BottomAction(
                icon = Icons.Outlined.ArrowForward,
                label = "Forward",
                enabled = canGoForward,
                onClick = onForward,
                textColor = textColor
            )
            BottomAction(
                icon = Icons.Outlined.Home,
                label = "Home",
                onClick = onHome,
                textColor = textColor
            )
            BottomAction(
                icon = Icons.Outlined.Language,
                label = "Tabs",
                badge = tabCount,
                onClick = onTabs,
                textColor = textColor
            )
            BottomAction(
                icon = Icons.Outlined.MoreVert,
                label = "Menu",
                onClick = onMenu,
                textColor = textColor
            )
        }
    }
}

@Composable
private fun BottomAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    textColor: Color,
    enabled: Boolean = true,
    badge: Int? = null
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor.copy(alpha = if (enabled) 0.90f else 0.26f),
                modifier = Modifier.size(21.dp)
            )
            if (badge != null && badge > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 7.dp, y = (-6).dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(BrowserBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.toString(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = textColor.copy(alpha = if (enabled) 0.55f else 0.24f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrowserHome(
    history: List<String>,
    adBlock: Boolean,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onMenu: () -> Unit,
    glass: GlassStyle,
    textColor: Color
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (query.isNotBlank()) {
            onSearch(query)
            keyboard?.hide()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0B10), Color(0xFF05060A))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 26.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrowserLogo(76.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "VidVibe Browser",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Chromium engine · private on-device session",
            fontSize = 11.sp,
            color = textColor.copy(alpha = 0.48f)
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(glass.bodyTop)
                .border(1.dp, glass.stroke, RoundedCornerShape(26.dp))
                .padding(start = 16.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = BrowserBlue,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                cursorBrush = SolidColor(BrowserBlue),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { submit() }),
                decorationBox = { inner ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = "Search the web",
                                fontSize = 14.sp,
                                color = textColor.copy(alpha = 0.35f)
                            )
                        }
                        inner()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BrowserBlue)
                    .clickable { submit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Search",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QUICK LINKS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = textColor.copy(alpha = 0.42f)
            )
        }
        Spacer(Modifier.height(10.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickLinks.forEach { link ->
                QuickLinkTile(
                    link = link,
                    glass = glass,
                    textColor = textColor,
                    onClick = { onOpen(link.url) }
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(glass.bodyTop)
                .border(1.dp, glass.stroke, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = null,
                tint = if (adBlock) BrowserGreen else textColor.copy(alpha = 0.45f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Ad blocker",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = if (adBlock) {
                        "Ads and trackers are blocked on every site."
                    } else {
                        "Off. Ads may appear on pages and videos."
                    },
                    fontSize = 10.5.sp,
                    color = textColor.copy(alpha = 0.46f)
                )
            }
            Switch(
                checked = adBlock,
                onCheckedChange = onToggleAdBlock
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                .clickable(onClick = onMenu)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = BrowserBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Sessions stay on this device",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Text(
                    text = "Sign in once, then sync the cookies to VidVibe downloads.",
                    fontSize = 10.5.sp,
                    color = textColor.copy(alpha = 0.44f)
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = textColor.copy(alpha = 0.42f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Clear",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrowserBlue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearHistory)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            history.take(6).forEach { url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { onOpen(url) }
                        .padding(horizontal = 8.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.35f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = prettyUrl(url),
                        fontSize = 11.5.sp,
                        color = textColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinkTile(
    link: QuickLink,
    glass: GlassStyle,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(98.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(glass.bodyTop)
            .border(1.dp, glass.stroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(link.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = link.label.take(1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = link.color
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text = link.label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BrowserError(
    message: String,
    onRetry: () -> Unit,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.35f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Page not available",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 11.5.sp,
            color = textColor.copy(alpha = 0.55f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BrowserBlue)
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Try again",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TabsOverlay(
    ui: BrowserUiState,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
    glass: GlassStyle,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07080C))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tabs",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = ui.tabs.size.toString(),
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.45f)
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close tabs",
                    tint = textColor,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(
                items = ui.tabs,
                key = { _, tab -> tab.id }
            ) { index, tab ->
                TabCard(
                    tab = tab,
                    active = index == ui.activeIndex,
                    onSelect = { onSelect(index) },
                    onClose = { onClose(index) },
                    glass = glass,
                    textColor = textColor
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(onClick = onNew),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New tab",
                        tint = BrowserBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "New tab",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.72f)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BrowserBlue.copy(alpha = 0.18f))
                .border(1.dp, BrowserBlue.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .clickable(onClick = onNew),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = BrowserBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "New tab",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun TabCard(
    tab: BrowserTab,
    active: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    glass: GlassStyle,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (active) glass.bodyTop else Color.White.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (active) BrowserBlue.copy(alpha = 0.55f) else glass.stroke,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BrowserBlue.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hostLetter(tab.url),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrowserBlue
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close tab",
                    tint = textColor.copy(alpha = 0.55f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = tab.title.ifBlank { tab.url.ifBlank { "New tab" } },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = prettyUrl(tab.url),
            fontSize = 9.5.sp,
            color = textColor.copy(alpha = 0.44f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BrowserMenu(
    ui: BrowserUiState,
    message: String?,
    onNewTab: () -> Unit,
    onTabs: () -> Unit,
    onToggleDesktop: (Boolean) -> Unit,
    onToggleAdBlock: (Boolean) -> Unit,
    onSyncCookies: () -> Unit,
    onShare: () -> Unit,
    onExternal: () -> Unit,
    onClearData: () -> Unit,
    onClose: () -> Unit,
    textColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrowserLogo(28.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "VidVibe Browser",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = "Chromium engine · ad blocking · cookie sync",
                    fontSize = 9.5.sp,
                    color = textColor.copy(alpha = 0.42f)
                )
            }
        }

        MenuRow(
            icon = Icons.Outlined.Add,
            label = "New tab",
            textColor = textColor,
            onClick = onNewTab
        )
        MenuRow(
            icon = Icons.Outlined.Language,
            label = "Tabs",
            trailing = ui.tabs.size.toString(),
            textColor = textColor,
            onClick = onTabs
        )
        MenuToggleRow(
            icon = Icons.Outlined.Language,
            label = "Desktop site",
            subtitle = "Request the full desktop layout",
            checked = ui.desktop,
            onCheckedChange = onToggleDesktop,
            textColor = textColor
        )
        MenuToggleRow(
            icon = Icons.Outlined.Block,
            label = "Ad blocker",
            subtitle = "Block ads and trackers, including YouTube ads",
            checked = ui.adBlock,
            onCheckedChange = onToggleAdBlock,
            textColor = textColor
        )
        MenuRow(
            icon = Icons.Outlined.Lock,
            label = "Sync sign-in to downloads",
            subtitle = "Save this site's cookies for the VidVibe downloader",
            textColor = textColor,
            onClick = onSyncCookies
        )
        MenuRow(
            icon = Icons.Outlined.Share,
            label = "Share page",
            textColor = textColor,
            onClick = onShare
        )
        MenuRow(
            icon = Icons.Outlined.OpenInNew,
            label = "Open in another app",
            textColor = textColor,
            onClick = onExternal
        )
        MenuRow(
            icon = Icons.Outlined.DeleteOutline,
            label = "Clear browsing data",
            subtitle = "Cookies, cache, history and saved form data",
            textColor = textColor,
            onClick = onClearData
        )
        MenuRow(
            icon = Icons.Outlined.Close,
            label = "Close browser",
            textColor = textColor,
            onClick = onClose
        )

        if (message != null) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrowserBlue.copy(alpha = 0.12f))
                    .border(
                        1.dp,
                        BrowserBlue.copy(alpha = 0.30f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Text(
                    text = message,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    textColor: Color,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.42f),
                    lineHeight = 13.sp
                )
            }
        }
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.48f)
            )
        }
    }
}

@Composable
private fun MenuToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.62f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.42f),
                lineHeight = 13.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * A tricolour orb with a white V. It reads as the familiar Chromium browser
 * language without copying another company's mark.
 */
@Composable
fun BrowserLogo(size: Dp) {
    Canvas(Modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val centerPoint = center
        val topLeft = Offset(centerPoint.x - radius, centerPoint.y - radius)
        val diameter = Size(radius * 2f, radius * 2f)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 212f,
            sweepAngle = 116f,
            useCenter = true,
            topLeft = topLeft,
            size = diameter
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 332f,
            sweepAngle = 116f,
            useCenter = true,
            topLeft = topLeft,
            size = diameter
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 92f,
            sweepAngle = 116f,
            useCenter = true,
            topLeft = topLeft,
            size = diameter
        )
        drawCircle(
            color = Color(0xFFF5F5F7),
            radius = radius * 0.46f,
            center = centerPoint
        )
        drawCircle(
            color = Color(0xFF4285F4),
            radius = radius * 0.38f,
            center = centerPoint
        )
        val mark = Path().apply {
            moveTo(centerPoint.x - radius * 0.17f, centerPoint.y - radius * 0.12f)
            lineTo(centerPoint.x, centerPoint.y + radius * 0.16f)
            lineTo(centerPoint.x + radius * 0.17f, centerPoint.y - radius * 0.12f)
        }
        drawPath(
            path = mark,
            color = Color.White,
            style = Stroke(
                width = radius * 0.15f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun prettyUrl(url: String): String {
    if (url.isBlank()) return "New tab"
    return url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')
        .let { if (it.length > 54) it.take(53) + "..." else it }
}

private fun hostLetter(url: String): String {
    val host = runCatching { Uri.parse(url).host }.getOrNull()
    return host?.firstOrNull()?.uppercaseChar()?.toString() ?: "V"
}
