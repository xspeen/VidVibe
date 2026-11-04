package com.vidvibe.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Playlist(
    val id: String,
    val name: String,
    val videoIds: Set<Long>
)

class PlaylistStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _playlists = MutableStateFlow(load())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    fun createPlaylist(name: String): Playlist? {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return null
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = cleanName,
            videoIds = emptySet()
        )
        update(_playlists.value + playlist)
        return playlist
    }

    fun addVideos(playlistId: String, videoIds: Set<Long>) {
        if (videoIds.isEmpty()) return
        update(
            _playlists.value.map { playlist ->
                if (playlist.id == playlistId) {
                    playlist.copy(videoIds = playlist.videoIds + videoIds)
                } else {
                    playlist
                }
            }
        )
    }

    fun renamePlaylist(playlistId: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        update(
            _playlists.value.map { playlist ->
                if (playlist.id == playlistId) playlist.copy(name = cleanName) else playlist
            }
        )
    }

    fun deletePlaylist(playlistId: String) {
        update(_playlists.value.filterNot { it.id == playlistId })
    }

    private fun update(playlists: List<Playlist>) {
        _playlists.value = playlists
        val array = JSONArray()
        playlists.forEach { playlist ->
            val objectValue = JSONObject()
            objectValue.put("id", playlist.id)
            objectValue.put("name", playlist.name)
            val ids = JSONArray()
            playlist.videoIds.forEach { ids.put(it) }
            objectValue.put("videoIds", ids)
            array.put(objectValue)
        }
        preferences.edit().putString(KEY_PLAYLISTS, array.toString()).apply()
    }

    private fun load(): List<Playlist> {
        val raw = preferences.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val idsArray = item.optJSONArray("videoIds") ?: JSONArray()
                    val ids = buildSet {
                        for (idIndex in 0 until idsArray.length()) {
                            add(idsArray.getLong(idIndex))
                        }
                    }
                    add(
                        Playlist(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            videoIds = ids
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS_NAME = "vidvibe_playlists"
        const val KEY_PLAYLISTS = "playlists"
    }
}
