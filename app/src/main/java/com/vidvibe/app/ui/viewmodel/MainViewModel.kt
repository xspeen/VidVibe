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
import com.vidvibe.app.data.Platform
import com.vidvibe.app.data.Playlist
import com.vidvibe.app.data.PlaylistStore
import com.vidvibe.app.data.SettingsStore
import com.vidvibe.app.data.TrashStore
import com.vidvibe.app.data.TrashedVideo
import com.vidvibe.app.data.VideoItem
import com.vidvibe.app.data.VideoRepository
import com.vidvibe.app.download.AudioEngine
import com.vidvibe.app.download.CookieStore
import com.vidvibe.app.download.DownloadQueue
import com.vidvibe.app.download.DownloadTask
import com.vidvibe.app.download.QueueStatus
import com.vidvibe.app.download.YtDlpManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Int, val status: String) : DownloadState
    data class Complete(val uri: Uri, val fileName: String) : DownloadState
    data class Error(val message: String) : DownloadState
}

/** Unseen counts shown as red badges on the library sections. */
data class LibraryBadges(
    val videos: Int = 0,
    val folders: Int = 0,
    val playlists: Int = 0,
    val audio: Int = 0
) {
    val hasAny: Boolean
        get() = videos > 0 || folders > 0 || playlists > 0 || audio > 0
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

    private val _audios = MutableStateFlow<List<VideoItem>>(emptyList())
    val audios: StateFlow<List<VideoItem>> = _audios.asStateFlow()

    private val _badges = MutableStateFlow(LibraryBadges())
    val badges: StateFlow<LibraryBadges> = _badges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    /** Live background queue; the downloader screen renders this list. */
    val queue: StateFlow<List<DownloadTask>> = DownloadQueue.tasks

    private val _pendingTab = MutableStateFlow<Int?>(null)
    val pendingTab: StateFlow<Int?> = _pendingTab.asStateFlow()

    private val _pasteToken = MutableStateFlow(0)
    val pasteToken: StateFlow<Int> = _pasteToken.asStateFlow()

    private var pasteCounter = 0

    private var refreshJob: Job? = null
    private var activeDownloadId: Long? = null
    private var pendingPermanentDeleteIds: Set<Long> = emptySet()

    init {
        refresh()
        viewModelScope.launch {
            if (settings.value.autoCookies) {
                withContext(Dispatchers.IO) {
                    CookieStore.autoImport(getApplication())
                }
            }
        }
        viewModelScope.launch {
            DownloadQueue.tasks.collect { tasks ->
                val id = activeDownloadId ?: return@collect
                val task = tasks.firstOrNull { it.id == id }
                if (task == null) {
                    activeDownloadId = null
                    return@collect
                }
                when (task.state) {
                    QueueStatus.Queued -> _downloadState.value =
                        DownloadState.Running(task.progress, "Queued")

                    QueueStatus.Running -> _downloadState.value =
                        DownloadState.Running(task.progress, task.status)

                    QueueStatus.Complete -> {
                        activeDownloadId = null
                        _downloadState.value = DownloadState.Complete(
                            Uri.EMPTY,
                            task.fileName ?: task.title
                        )
                        noteNewDownload(task)
                        loadLibraryNow()
                    }

                    QueueStatus.Failed -> {
                        activeDownloadId = null
                        _downloadState.value = DownloadState.Error(
                            task.error ?: "The download could not be completed."
                        )
                    }

                    QueueStatus.Cancelled -> {
                        activeDownloadId = null
                        _downloadState.value = DownloadState.Idle
                    }
                }
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isLoading.value = true
            loadLibraryNow()
            _isLoading.value = false
        }
    }

    fun markSectionSeen(section: Int) {
        _badges.update {
            when (section) {
                0 -> it.copy(videos = 0)
                1 -> it.copy(folders = 0)
                2 -> it.copy(playlists = 0)
                3 -> it.copy(audio = 0)
                else -> it
            }
        }
    }

    private fun noteNewDownload(task: DownloadTask) {
        _badges.update {
            if (task.isAudio) {
                it.copy(audio = (it.audio + 1).coerceAtMost(99))
            } else {
                it.copy(
                    videos = (it.videos + 1).coerceAtMost(99),
                    folders = (it.folders + 1).coerceAtMost(99),
                    playlists = (it.playlists + 1).coerceAtMost(99)
                )
            }
        }
    }

    fun playAudio(tracks: List<VideoItem>, index: Int) {
        if (tracks.isEmpty()) return
        AudioEngine.play(getApplication(), tracks, index.coerceIn(0, tracks.lastIndex))
    }

    fun moveToTrash(videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        trashStore.add(videos)
        viewModelScope.launch { loadLibraryNow() }
    }

    fun restoreFromTrash(ids: Set<Long>) {
        if (ids.isEmpty()) return
        trashStore.remove(ids)
        viewModelScope.launch { loadLibraryNow() }
    }

    suspend fun deletePermanently(ids: Set<Long>): DeleteResult {
        if (ids.isEmpty()) return DeleteResult.Deleted
        val entries = trashStore.entriesFor(ids)
        if (entries.isEmpty()) {
            trashStore.remove(ids)
            loadLibraryNow()
            return DeleteResult.Deleted
        }

        val result = repository.deleteVideos(entries.map { Uri.parse(it.uri) })
        when (result) {
            is DeleteResult.Deleted -> {
                trashStore.remove(ids)
                loadLibraryNow()
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
            loadLibraryNow()
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

    fun startDownload(
        url: String,
        qualityHeight: Int,
        isAudio: Boolean = Platform.fromUrl(url).audio
    ) {
        val cleanUrl = url.trim()
        val parsed = runCatching { Uri.parse(cleanUrl) }.getOrNull()
        if (
            parsed == null ||
            parsed.scheme !in setOf("http", "https") ||
            parsed.host.isNullOrBlank()
        ) {
            _downloadState.value = DownloadState.Error(
                "Paste a valid http or https link."
            )
            return
        }

        if (settings.value.wifiOnly && !isOnWifi()) {
            _downloadState.value = DownloadState.Error(
                "Wi-Fi only is enabled. Connect to Wi-Fi or turn the option off in Download settings."
            )
            return
        }

        val id = DownloadQueue.enqueue(getApplication(), cleanUrl, qualityHeight, isAudio)
        activeDownloadId = id
        _downloadState.value = DownloadState.Running(0, "Queued")
    }

    fun cancelQueuedDownload(id: Long) {
        DownloadQueue.cancel(getApplication(), id)
    }

    fun retryQueuedDownload(id: Long) {
        DownloadQueue.retry(getApplication(), id)
    }

    fun removeQueuedDownload(id: Long) {
        DownloadQueue.remove(id)
    }

    fun clearFinishedDownloads() {
        DownloadQueue.clearFinished()
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

    fun setOnboardingSeen(seen: Boolean) {
        viewModelScope.launch { settingsStore.setOnboardingSeen(seen) }
    }

    fun setNotificationsPrompted(prompted: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsPrompted(prompted) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setNotificationsEnabled(enabled) }
    }

    fun setBackgroundDownloads(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setBackgroundDownloads(enabled) }
    }

    fun setAutoCookies(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoCookies(enabled) }
    }

    fun setDefaultQuality(height: Int) {
        viewModelScope.launch { settingsStore.setDefaultQuality(height) }
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
        iconPack: String,
        appTheme: String
    ) {
        viewModelScope.launch {
            settingsStore.setCustomization(
                animationStyle = animationStyle,
                appFont = appFont,
                backgroundLook = backgroundLook,
                textColor = textColor,
                glassColor = glassColor,
                iconPack = iconPack,
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

    private suspend fun loadLibraryNow() {
        val all = repository.getVideos()
        trashStore.prune(all.map { it.id }.toSet())
        _videos.value = all.filterNot { it.id in trashStore.trashedIds }
        _audios.value = repository.getAudios()
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
