package com.vidvibe.app.browser

import android.net.Uri

/**
 * Request-level ad and tracker blocking for the VidVibe browser.
 *
 * The block list targets advertising and measurement hosts, plus the small set
 * of YouTube endpoints that deliver pre-roll, mid-roll and in-feed ads. Media
 * delivery hosts are never blocked, so normal playback and downloads keep
 * working. The companion JavaScript only removes ad UI and fast-forwards an
 * active ad slot; it does not touch page content.
 */
object AdBlocker {

    private val blockedHosts = listOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "google-analytics.com",
        "googletagservices.com",
        "googletagmanager.com",
        "adservice.google.com",
        "ads.youtube.com",
        "adsystem.com",
        "amazon-adsystem.com",
        "adnxs.com",
        "adsrvr.org",
        "criteo.com",
        "criteo.net",
        "moatads.com",
        "scorecardresearch.com",
        "taboola.com",
        "outbrain.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "applovin.com",
        "inmobi.com",
        "mopub.com",
        "unityads.unity3d.com",
        "analytics.tiktok.com",
        "ads.tiktok.com",
        "analytics.twitter.com",
        "ads.linkedin.com",
        "ads.pinterest.com",
        "ad.doubleclick.net",
        "pagead2.googlesyndication.com",
        "partner.googleadservices.com",
        "pubads.g.doubleclick.net",
        "securepubads.g.doubleclick.net",
        "static.doubleclick.net",
        "tpc.googlesyndication.com"
    )

    private val youtubeBlockedPaths = listOf(
        "/pagead/",
        "/api/stats/ads",
        "/api/stats/atr",
        "/ptracking",
        "/get_midroll_info",
        "/ad_break",
        "/adunit",
        "/youtubei/v1/player/ad"
    )

    fun isBlocked(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false

        if (blockedHosts.any { blocked -> host == blocked || host.endsWith(".$blocked") }) {
            return true
        }

        if (host == "youtube.com" || host.endsWith(".youtube.com")) {
            val path = (uri.path ?: "").lowercase()
            if (youtubeBlockedPaths.any { path.contains(it) }) return true
            val query = (uri.query ?: "").lowercase()
            if (path.contains("videoplayback") && query.contains("adformat=")) return true
        }

        return false
    }

    /**
     * Runs on every YouTube page load and on SPA navigation. Ad slots are
     * removed, skip buttons are pressed, and a playing ad is muted and jumped
     * to its end so the requested video starts immediately.
     */
    fun youtubeScript(): String = """
        (function () {
          var kill = function () {
            try {
              var players = document.querySelectorAll('.ad-showing, .ad-interrupting');
              for (var i = 0; i < players.length; i++) {
                var video = players[i].querySelector('video');
                if (video) {
                  video.muted = true;
                  try {
                    if (video.duration && isFinite(video.duration)) {
                      video.currentTime = video.duration;
                    }
                  } catch (e) {}
                  try { video.playbackRate = 16; } catch (e) {}
                }
                var skips = players[i].querySelectorAll(
                  '.ytp-ad-skip-button, .ytp-skip-ad-button, ' +
                  '.ytp-ad-skip-button-modern, .ytp-ad-skip-button-slot button'
                );
                for (var j = 0; j < skips.length; j++) {
                  try { skips[j].click(); } catch (e) {}
                }
                var overlays = players[i].querySelectorAll(
                  '.ytp-ad-overlay-container, .ytp-ad-image-overlay, ' +
                  '.ytp-ad-text-overlay, .ytp-ad-player-overlay-layout'
                );
                for (var k = 0; k < overlays.length; k++) {
                  try { overlays[k].remove(); } catch (e) {}
                }
              }
              var slots = document.querySelectorAll(
                'ytd-ad-slot-renderer, ytd-in-feed-ad-layout-renderer, ' +
                'ytd-promoted-sparkles-web-renderer, ytd-display-ad-renderer, ' +
                'ytd-banner-promo-renderer, ytd-statement-banner-renderer, ' +
                'ytd-companion-slot-renderer, #masthead-ad, #player-ads, ' +
                '#panels-full-bleed-ad-container'
              );
              for (var s = 0; s < slots.length; s++) {
                try { slots[s].remove(); } catch (e) {}
              }
            } catch (e) {}
          };
          kill();
          if (!window.__vidvibeAdTimer) {
            window.__vidvibeAdTimer = setInterval(kill, 800);
            window.addEventListener('yt-navigate-finish', kill);
            window.addEventListener('yt-page-data-updated', kill);
          }
        })();
    """.trimIndent()
}
