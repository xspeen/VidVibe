package com.vidvibe.app.data

import android.net.Uri

/**
 * Brand of a pasted link. Audio-first services are flagged so the downloader
 * can switch to audio mode automatically instead of asking the user.
 */
enum class Platform(
    val label: String,
    val audio: Boolean
) {
    YouTube("YouTube", false),
    YouTubeMusic("YouTube Music", true),
    Spotify("Spotify", true),
    AppleMusic("Apple Music", true),
    SoundCloud("SoundCloud", true),
    Deezer("Deezer", true),
    Tidal("Tidal", true),
    Bandcamp("Bandcamp", true),
    Audiomack("Audiomack", true),
    Instagram("Instagram", false),
    TikTok("TikTok", false),
    Facebook("Facebook", false),
    X("X", false),
    Twitter("Twitter", false),
    Reddit("Reddit", false),
    Vimeo("Vimeo", false),
    Twitch("Twitch", false),
    Threads("Threads", false),
    Other("Media link", false);

    companion object {
        fun fromUrl(url: String): Platform {
            val host = runCatching { Uri.parse(url).host }
                .getOrNull()
                ?.lowercase()
                .orEmpty()
                .removePrefix("www.")
            return detect(host)
        }

        fun detect(host: String): Platform {
            val h = host.lowercase().removePrefix("www.")
            return when {
                h == "music.youtube.com" || h.startsWith("music.youtube.") -> YouTubeMusic
                h.contains("spotify") -> Spotify
                h == "music.apple.com" || h.startsWith("music.apple.") -> AppleMusic
                h.contains("soundcloud") -> SoundCloud
                h.contains("deezer") -> Deezer
                h.contains("tidal") -> Tidal
                h.contains("bandcamp") -> Bandcamp
                h.contains("audiomack") -> Audiomack
                h.contains("youtu") -> YouTube
                h.contains("instagram") -> Instagram
                h.contains("tiktok") -> TikTok
                h.contains("facebook") || h.contains("fb.watch") || h.contains("fb.com") -> Facebook
                h == "x.com" || h.endsWith(".x.com") -> X
                h.contains("twitter") -> Twitter
                h.contains("reddit") || h.contains("redd.it") -> Reddit
                h.contains("vimeo") -> Vimeo
                h.contains("twitch") -> Twitch
                h.contains("threads") -> Threads
                else -> Other
            }
        }
    }
}
