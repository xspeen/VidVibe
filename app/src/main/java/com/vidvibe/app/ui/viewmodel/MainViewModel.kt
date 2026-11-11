package com.vidvibe.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidvibe.app.data.AppSettings
import com.vidvibe.app.data.DeleteResult
import com.vidvibe.app.data.Playlist
import com.vidvibe.app.data.PlaylistStore
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.data.TrashStore
import com.vidvibe.app.data.TrashedVideo
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.VideoRepository
import com.vidvibe.app.download.YtDlpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Int, val status: String) : DownloadState
    data class Complete(val uri: Uri, val fileName: String) : DownloadState
    data class Error(val message: String) : DownloadState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val settingsStore = SettingsStore(application)
    private val playlistStore = PlaylistStore(application)
    private val trashStore = TrashStore(application)
    private val ytDlpManager = YtDlpManager(application)

    val settings: StateFlow<AppSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val settingsLoaded: StateFlow<Boolean> = settingsStore.settings
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val playlists: StateFlow<List<Playlist>> = playlistStore.playlists
    val trash: StateFlow<List<TrashedVideo>> = trashStore.trash

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _pendingTab = MutableStateFlow<Int?>(null)
    val pendingTab: StateFlow<Int?> = _pendingTab.asStateFlow()

    private val _pasteToken = MutableStateFlow(0)
    val pasteToken: StateFlow<Int> = _pasteToken.asStateFlow()

    private val _cookiesImported = MutableStateFlow(YtDlpManager.hasCookies(application))
    val cookiesImported: StateFlow<Boolean> = _cookiesImported.asStateFlow()

    private val _cookieError = MutableStateFlow<String?>(null)
    val cookieError: StateFlow<String?> = _cookieError.asStateFlow()

    private var pasteCounter = 0

    private var refreshJob: Job? = null
    private var downloadJob: Job? = null
    private var pendingPermanentDeleteIds: Set<Long> = emptySet()

    init {
        refresh()
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            val all = repository.getVideos()
            trashStore.prune(all.map { it.id }.toSet())
            _videos.value = all.filterNot { it.id in trashStore.trashedIds }
            _isLoading.value = false
        }
    }

    fun moveToTrash(videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        trashStore.add(videos)
        viewModelScope.launch { loadVideosNow() }
    }

    fun restoreFromTrash(ids: Set<Long>) {
        if (ids.isEmpty()) return
        trashStore.remove(ids)
        viewModelScope.launch { loadVideosNow() }
    }

    suspend fun deletePermanently(ids: Set<Long>): DeleteResult {
        if (ids.isEmpty()) return DeleteResult.Deleted
        val entries = trashStore.entriesFor(ids)
        if (entries.isEmpty()) {
            trashStore.remove(ids)
            loadVideosNow()
            return DeleteResult.Deleted
        }

        val result = repository.deleteVideos(entries.map { Uri.parse(it.uri) })
        when (result) {
            is DeleteResult.Deleted -> {
                trashStore.remove(ids)
                loadVideosNow()
            }
            is DeleteResult.NeedsConfirmation -> {
                pendingPermanentDeleteIds = ids
            }
            is DeleteResult.Failed -> Unit
        }
        return result
    }

    suspend fun deleteVideos(uris: List<Uri>): DeleteResult {
        val result = repository.deleteVideos(uris)
        if (result is DeleteResult.Deleted) {
            loadVideosNow()
        }
        return result
    }

    fun onSystemDeleteConfirmed() {
        if (pendingPermanentDeleteIds.isNotEmpty()) {
            trashStore.remove(pendingPermanentDeleteIds)
            pendingPermanentDeleteIds = emptySet()
        }
        refresh()
    }

    fun onSystemDeleteCancelled() {
        pendingPermanentDeleteIds = emptySet()
    }

    /**
     * Copies a user-picked Netscape cookies.txt into app storage. yt-dlp reads
     * it for every site, which is what makes private Instagram posts and
     * Pornhub logins reachable.
     */
    fun importCookies(uri: Uri) {
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) {
                runCatching {
                    val app = getApplication<Application>()
                    val target = YtDlpManager.cookiesFile(app)
                    val stream = app.contentResolver.openInputStream(uri)
                        ?: return@runCatching false
                    stream.use { source ->
                        target.outputStream().use { sink -> source.copyTo(sink) }
                    }
                    val valid = target.isFile && target.length() > 0L && looksLikeCookies(target)
                    if (!valid) target.delete()
                    valid
                }.getOrDefault(false)
            }
            _cookiesImported.value = imported
            if (!imported) {
                _cookieError.value =
                    "That file is not a Netscape cookies.txt export."
            }
        }
    }

    fun clearCookies() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching { YtDlpManager.cookiesFile(getApplication()).delete() }
            }
            _cookiesImported.value = false
        }
    }

    fun consumeCookieError() {
        _cookieError.value = null
    }

    private fun looksLikeCookies(file: File): Boolean = runCatching {
        file.useLines { lines ->
            lines.take(500).any { line ->
                !line.startsWith("#") && line.count { it == '\t' } >= 6
            }
        }
    }.getOrDefault(false)

    fun startDownload(url: String, qualityHeight: Int) {
        if (downloadJob?.isActive == true) return

        val cleanUrl = url.trim()
        val parsed = runCatching { Uri.parse(cleanUrl) }.getOrNull()
        if (
            parsed == null ||
            parsed.scheme !in setOf("http", "https") ||
            parsed.host.isNullOrBlank()
        ) {
            _downloadState.value = DownloadState.Error(
                "Paste a valid http or https video link."
            )
            return
        }

        if (settings.value.wifiOnly && !isOnWifi()) {
            _downloadState.value = DownloadState.Error(
                "Wi-Fi only is enabled. Connect to Wi-Fi or turn the option off in Download settings."
            )
            return
        }

        downloadJob = viewModelScope.launch {
            _downloadState.value = DownloadState.Running(0, "Preparing download...")
            try {
                val file = ytDlpManager.downloadVideo(
                    url = cleanUrl,
                    qualityHeight = qualityHeight
                ) { percent, status ->
                    _downloadState.value = DownloadState.Running(percent, status)
                }

                val galleryName = if (file.name.startsWith("VidVibe_")) {
                    file.name
                } else {
                    "VidVibe_${file.name}"
                }
                val mimeType = repository.mimeTypeFor(galleryName)
                val galleryUri = withContext(Dispatchers.IO) {
                    repository.saveToGallery(file, galleryName, mimeType)
                }

                runCatching { file.delete() }
                _downloadState.value = DownloadState.Complete(galleryUri, galleryName)
                loadVideosNow()
            } catch (error: Throwable) {
                val message = error.message
                    ?.substringBefore('\n')
                    ?.takeIf { it.isNotBlank() }
                    ?: "The download could not be completed."
                _downloadState.value = DownloadState.Error(message)
            }
        }
    }

    fun resetDownload() {
        if (_downloadState.value !is DownloadState.Running) {
            _downloadState.value = DownloadState.Idle
        }
    }

    fun clearDownloadAfterSuccess() {
        if (_downloadState.value is DownloadState.Complete) {
            _downloadState.value = DownloadState.Idle
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { settingsStore.setFontSize(sizeSp) }
    }

    fun setLiquidGlass(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setLiquidGlass(enabled) }
    }

    fun setBackgroundColor(color: Int) {
        viewModelScope.launch { settingsStore.setBackgroundColor(color) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setWifiOnly(enabled) }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAppLock(enabled) }
    }

    fun setOnboardingSeen(seen: Boolean) {
        viewModelScope.launch { settingsStore.setOnboardingSeen(seen) }
    }

    fun setFloatingWindowEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setFloatingWindowEnabled(enabled) }
    }

    fun requestTab(index: Int) {
        _pendingTab.value = index.coerceIn(0, 3)
    }

    fun consumePendingTab() {
        _pendingTab.value = null
    }

    fun requestPaste() {
        pasteCounter += 1
        _pendingTab.value = 1
        _pasteToken.value = pasteCounter
    }

    fun consumePaste() {
        _pasteToken.value = 0
    }

    fun setCustomization(
        animationStyle: String,
        appFont: String,
        backgroundLook: String,
        textColor: String,
        glassColor: String,
        appTheme: String
    ) {
        viewModelScope.launch {
            settingsStore.setCustomization(
                animationStyle = animationStyle,
                appFont = appFont,
                backgroundLook = backgroundLook,
                textColor = textColor,
                glassColor = glassColor,
                appTheme = appTheme
            )
        }
    }

    fun createPlaylist(name: String): Playlist? = playlistStore.createPlaylist(name)

    fun addVideosToPlaylist(playlistId: String, videoIds: Set<Long>) {
        playlistStore.addVideos(playlistId, videoIds)
    }

    fun deletePlaylist(playlistId: String) {
        playlistStore.deletePlaylist(playlistId)
    }

    private suspend fun loadVideosNow() {
        val all = repository.getVideos()
        trashStore.prune(all.map { it.id }.toSet())
        _videos.value = all.filterNot { it.id in trashStore.trashedIds }
    }

    private fun isOnWifi(): Boolean {
        val connectivity = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun pendingIntentSender(result: DeleteResult.NeedsConfirmation): IntentSender {
        return result.intentSender
    }
}
