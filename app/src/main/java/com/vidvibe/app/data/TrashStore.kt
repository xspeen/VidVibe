package com.vidvibe.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * A video that has been moved to Trash. The file itself stays in MediaStore
 * until the user chooses Delete Permanently, so Restore is instant and safe.
 */
data class TrashedVideo(
    val id: Long,
    val uri: String,
    val displayName: String,
    val dateAddedSeconds: Long,
    val durationMs: Long,
    val bucketName: String,
    val sizeBytes: Long,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val trashedAt: Long
) {
    fun toVideoItem(): VideoItem = VideoItem(
        id = id,
        uri = Uri.parse(uri),
        displayName = displayName,
        dateAddedSeconds = dateAddedSeconds,
        durationMs = durationMs,
        bucketName = bucketName,
        sizeBytes = sizeBytes,
        relativePath = relativePath,
        width = width,
        height = height
    )
}

class TrashStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _trash = MutableStateFlow(load())
    val trash: StateFlow<List<TrashedVideo>> = _trash.asStateFlow()

    val trashedIds: Set<Long>
        get() = _trash.value.map { it.id }.toSet()

    fun add(videos: List<VideoItem>) {
        if (videos.isEmpty()) return
        val now = System.currentTimeMillis()
        val existing = _trash.value.associateBy { it.id }.toMutableMap()
        videos.forEach { video ->
            existing[video.id] = TrashedVideo(
                id = video.id,
                uri = video.uri.toString(),
                displayName = video.displayName,
                dateAddedSeconds = video.dateAddedSeconds,
                durationMs = video.durationMs,
                bucketName = video.bucketName,
                sizeBytes = video.sizeBytes,
                relativePath = video.relativePath,
                width = video.width,
                height = video.height,
                trashedAt = now
            )
        }
        update(existing.values.sortedByDescending { it.trashedAt })
    }

    fun remove(ids: Set<Long>) {
        if (ids.isEmpty()) return
        update(_trash.value.filterNot { it.id in ids })
    }

    /**
     * Drops entries whose MediaStore row no longer exists (for example when the
     * file was deleted by another app or by the system).
     */
    fun prune(existingIds: Set<Long>) {
        val current = _trash.value
        val kept = current.filter { it.id in existingIds }
        if (kept.size != current.size) update(kept)
    }

    fun clear() {
        update(emptyList())
    }

    fun entriesFor(ids: Set<Long>): List<TrashedVideo> =
        _trash.value.filter { it.id in ids }

    private fun update(items: List<TrashedVideo>) {
        _trash.value = items
        val array = JSONArray()
        items.forEach { item ->
            val json = JSONObject()
            json.put("id", item.id)
            json.put("uri", item.uri)
            json.put("displayName", item.displayName)
            json.put("dateAddedSeconds", item.dateAddedSeconds)
            json.put("durationMs", item.durationMs)
            json.put("bucketName", item.bucketName)
            json.put("sizeBytes", item.sizeBytes)
            json.put("relativePath", item.relativePath)
            json.put("width", item.width)
            json.put("height", item.height)
            json.put("trashedAt", item.trashedAt)
            array.put(json)
        }
        preferences.edit().putString(KEY_TRASH, array.toString()).apply()
    }

    private fun load(): List<TrashedVideo> {
        val raw = preferences.getString(KEY_TRASH, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    val id = json.optLong("id", -1L)
                    if (id < 0L) continue
                    add(
                        TrashedVideo(
                            id = id,
                            uri = json.optString("uri"),
                            displayName = json.optString("displayName", "Untitled video"),
                            dateAddedSeconds = json.optLong("dateAddedSeconds"),
                            durationMs = json.optLong("durationMs"),
                            bucketName = json.optString("bucketName", "Videos"),
                            sizeBytes = json.optLong("sizeBytes"),
                            relativePath = json.optString("relativePath"),
                            width = json.optInt("width"),
                            height = json.optInt("height"),
                            trashedAt = json.optLong("trashedAt")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "vidvibe_trash"
        const val KEY_TRASH = "trashed_videos"
    }
}
