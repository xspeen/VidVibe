package com.vidvibe.app.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import android.widget.FrameLayout
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.vidvibe.app.download.CookieStore
import java.io.ByteArrayInputStream

data class BrowserTab(
    val id: Int,
    val url: String,
    val title: String,
    val progress: Int,
    val loading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val error: String?
)

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeIndex: Int = 0,
    val home: Boolean = true,
    val adBlock: Boolean = true,
    val desktop: Boolean = false
) {
    val active: BrowserTab? get() = tabs.getOrNull(activeIndex)
}

/**
 * Owns every tab's WebView and keeps exactly one attached to [host]. The
 * Compose layer only renders [state]; all page lifecycle work stays here so
 * switching tabs or opening the home screen never reloads a page.
 */
class BrowserController(
    private val context: Context,
    private val prefs: BrowserPreferences,
    adBlockEnabled: Boolean,
    desktopEnabled: Boolean
) {

    private class Tab(val id: Int, val webView: WebView) {
        var url: String = ""
        var title: String = "New tab"
        var progress: Int = 0
        var loading: Boolean = false
        var canGoBack: Boolean = false
        var canGoForward: Boolean = false
        var error: String? = null
    }

    private val tabs = mutableListOf<Tab>()
    private var nextId = 1
    private var activeIndex = -1
    private var homeVisible = true
    private var adBlock = adBlockEnabled
    private var desktop = desktopEnabled

    private val _state = mutableStateOf(BrowserUiState(adBlock = adBlockEnabled, desktop = desktopEnabled))
    val state: State<BrowserUiState> = _state

    val host = FrameLayout(context).apply {
        setBackgroundColor(android.graphics.Color.BLACK)
    }

    var onFileChooser: ((ValueCallback<Array<Uri>>) -> Unit)? = null
    var onToast: ((String) -> Unit)? = null

    init {
        newTab(null)
    }

    fun newTab(url: String? = null) {
        val tab = Tab(nextId++, createWebView())
        tabs += tab
        activeIndex = tabs.lastIndex
        attachActive()
        if (url.isNullOrBlank()) {
            tab.url = ""
            tab.title = "New tab"
            homeVisible = true
            publish()
        } else {
            homeVisible = false
            tab.url = url
            tab.title = url
            tab.webView.loadUrl(url)
            publish()
        }
    }

    fun selectTab(index: Int) {
        if (index !in tabs.indices || index == activeIndex) return
        activeIndex = index
        attachActive()
        homeVisible = tabs[index].url.isBlank()
        publish()
    }

    fun closeTab(index: Int) {
        val tab = tabs.getOrNull(index) ?: return
        val wasActive = index == activeIndex
        tabs.removeAt(index)
        detach(tab.webView)
        runCatching {
            tab.webView.stopLoading()
            tab.webView.destroy()
        }
        if (tabs.isEmpty()) {
            activeIndex = -1
            newTab(null)
            return
        }
        if (wasActive) {
            activeIndex = activeIndex.coerceAtMost(tabs.lastIndex)
            attachActive()
        } else if (index < activeIndex) {
            activeIndex -= 1
        }
        homeVisible = tabs[activeIndex].url.isBlank()
        publish()
    }

    fun load(input: String) {
        val tab = tabs.getOrNull(activeIndex) ?: return
        val target = resolve(input)
        homeVisible = false
        tab.url = target
        tab.title = target
        tab.error = null
        tab.loading = true
        tab.progress = 0
        tab.webView.loadUrl(target)
        publish()
    }

    fun goBack() {
        val tab = tabs.getOrNull(activeIndex) ?: return
        if (tab.webView.canGoBack()) tab.webView.goBack()
    }

    fun goForward() {
        val tab = tabs.getOrNull(activeIndex) ?: return
        if (tab.webView.canGoForward()) tab.webView.goForward()
    }

    fun reload() {
        tabs.getOrNull(activeIndex)?.webView?.reload()
    }

    fun stopLoading() {
        tabs.getOrNull(activeIndex)?.webView?.stopLoading()
    }

    fun goHome() {
        homeVisible = true
        publish()
    }

    fun canGoBack(): Boolean =
        tabs.getOrNull(activeIndex)?.webView?.canGoBack() == true

    fun currentUrl(): String =
        tabs.getOrNull(activeIndex)?.let { it.webView.url ?: it.url }.orEmpty()

    fun setAdBlock(enabled: Boolean) {
        adBlock = enabled
        prefs.adBlock = enabled
        publish()
        if (enabled) {
            tabs.getOrNull(activeIndex)?.webView?.let {
                it.evaluateJavascript(AdBlocker.youtubeScript(), null)
            }
        }
    }

    fun setDesktop(enabled: Boolean) {
        desktop = enabled
        prefs.desktopMode = enabled
        val agent = if (enabled) DESKTOP_UA else MOBILE_UA
        tabs.forEach { it.webView.settings.userAgentString = agent }
        tabs.getOrNull(activeIndex)?.webView?.reload()
        publish()
    }

    /**
     * Writes the live Chromium cookie jar for the current site into the
     * downloader's cookies.txt. Called automatically after every page load and
     * on demand from the menu.
     */
    fun syncCookies(): Int {
        val url = currentUrl()
        if (!url.startsWith("http")) return 0
        runCatching { CookieManager.getInstance().flush() }
        return CookieStore.mergeFromWebView(context, url)
    }

    fun clearBrowsingData() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        runCatching { WebStorage.getInstance().deleteAllData() }
        runCatching {
            WebViewDatabase.getInstance(context).clearFormData()
            WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
        }
        tabs.forEach { tab ->
            runCatching {
                tab.webView.clearHistory()
                tab.webView.clearCache(true)
                tab.webView.clearFormData()
            }
        }
        prefs.clearHistory()
        publish()
    }

    fun destroy() {
        host.removeAllViews()
        tabs.forEach { tab ->
            detach(tab.webView)
            runCatching {
                tab.webView.stopLoading()
                tab.webView.destroy()
            }
        }
        tabs.clear()
        activeIndex = -1
    }

    private fun attachActive() {
        host.removeAllViews()
        val tab = tabs.getOrNull(activeIndex) ?: return
        detach(tab.webView)
        host.addView(
            tab.webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun detach(view: WebView) {
        (view.parent as? ViewGroup)?.removeView(view)
    }

    private fun publish() {
        val snapshot = tabs.map { tab ->
            BrowserTab(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                progress = tab.progress,
                loading = tab.loading,
                canGoBack = tab.canGoBack,
                canGoForward = tab.canGoForward,
                error = tab.error
            )
        }
        _state.value = BrowserUiState(
            tabs = snapshot,
            activeIndex = activeIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0)),
            home = homeVisible,
            adBlock = adBlock,
            desktop = desktop
        )
    }

    private fun updateTab(view: WebView, block: (Tab) -> Unit) {
        val tab = tabs.firstOrNull { it.webView === view } ?: return
        block(tab)
        if (tabs.getOrNull(activeIndex) === tab) {
            publish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        return WebView(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = if (desktop) DESKTOP_UA else MOBILE_UA
                builtInZoomControls = true
                displayZoomControls = false
                textZoom = 100
                allowFileAccess = false
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            val webView = this
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val target = request.url.toString()
                    if (adBlock && AdBlocker.isBlocked(target)) return true
                    if (target.startsWith("http://") || target.startsWith("https://")) {
                        return false
                    }
                    return openExternal(target)
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (adBlock && AdBlocker.isBlocked(url)) return true
                    if (url.startsWith("http://") || url.startsWith("https://")) return false
                    return openExternal(url)
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    if (adBlock && AdBlocker.isBlocked(request.url.toString())) {
                        return WebResourceResponse(
                            "text/plain",
                            "utf-8",
                            ByteArrayInputStream(ByteArray(0))
                        )
                    }
                    return null
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String?,
                    favicon: Bitmap?
                ) {
                    updateTab(view) { tab ->
                        if (!url.isNullOrBlank()) tab.url = url
                        tab.loading = true
                        tab.progress = 0
                        tab.error = null
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    val finalUrl = url ?: view.url ?: ""
                    updateTab(view) { tab ->
                        if (finalUrl.isNotBlank()) tab.url = finalUrl
                        tab.title = view.title?.takeIf { it.isNotBlank() }
                            ?: tab.title
                        tab.loading = false
                        tab.progress = 100
                        tab.canGoBack = view.canGoBack()
                        tab.canGoForward = view.canGoForward()
                    }
                    if (finalUrl.startsWith("http")) {
                        runCatching { CookieManager.getInstance().flush() }
                        CookieStore.mergeFromWebView(context, finalUrl)
                        prefs.addHistory(finalUrl)
                    }
                    if (adBlock) {
                        view.evaluateJavascript(AdBlocker.youtubeScript(), null)
                    }
                }

                override fun doUpdateVisitedHistory(
                    view: WebView,
                    url: String?,
                    isReload: Boolean
                ) {
                    updateTab(view) { tab ->
                        tab.canGoBack = view.canGoBack()
                        tab.canGoForward = view.canGoForward()
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        updateTab(view) { tab ->
                            tab.loading = false
                            tab.error = error.description?.toString()
                                ?: "This page could not be loaded."
                        }
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    updateTab(view) { tab ->
                        tab.progress = newProgress
                        tab.loading = newProgress < 100
                    }
                }

                override fun onReceivedTitle(view: WebView, title: String?) {
                    updateTab(view) { tab ->
                        tab.title = title?.takeIf { it.isNotBlank() } ?: tab.title
                    }
                }

                override fun onShowFileChooser(
                    view: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: WebChromeClient.FileChooserParams
                ): Boolean {
                    val handler = onFileChooser ?: return false
                    handler(filePathCallback)
                    return true
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                runCatching {
                    val name = URLUtil.guessFileName(url, contentDisposition, mimetype)
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setMimeType(mimetype)
                        .setTitle(name)
                        .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                        )
                        .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            name
                        )
                    request.addRequestHeader("User-Agent", userAgent)
                    CookieManager.getInstance().getCookie(url)?.let { cookie ->
                        request.addRequestHeader("Cookie", cookie)
                    }
                    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE)
                        as DownloadManager
                    manager.enqueue(request)
                    onToast?.invoke("Downloading $name")
                }.onFailure {
                    onToast?.invoke("This file could not be downloaded.")
                }
            }
        }
    }

    private fun openExternal(raw: String): Boolean {
        return runCatching {
            val intent = if (raw.startsWith("intent://")) {
                Intent.parseUri(raw, Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(raw))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun resolve(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return "https://www.google.com/"
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        if (trimmed.startsWith("about:") || trimmed.startsWith("file:")) return trimmed
        if (trimmed.contains(" ") || !trimmed.contains(".")) {
            return "https://www.google.com/search?q=" + Uri.encode(trimmed)
        }
        return "https://$trimmed"
    }

    companion object {
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    }
}
