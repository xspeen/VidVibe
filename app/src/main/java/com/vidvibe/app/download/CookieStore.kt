package com.vidvibe.app.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import java.io.File

/**
 * Automatic cookie handling. VidVibe never asks the user to export a file by
 * hand: sign-in happens inside the app, the WebView's cookie jar is written to
 * a Netscape cookies.txt that yt-dlp already understands, and a cookies.txt
 * dropped in Downloads is picked up on the next launch.
 */
object CookieStore {

    private const val HALF_YEAR_SECONDS = 60L * 60L * 24L * 180L

    fun cookiesFile(context: Context): File = YtDlpManager.cookiesFile(context)

    fun hasCookies(context: Context): Boolean = cookiesFile(context).let {
        it.isFile && it.length() > 0L
    }

    fun looksLikeCookies(file: File): Boolean = runCatching {
        file.useLines { lines ->
            lines.take(500).any { line ->
                !line.startsWith("#") && line.count { it == '\t' } >= 6
            }
        }
    }.getOrDefault(false)

    /**
     * Writes the WebView cookie jar for [url] as Netscape-format rows. Returns
     * the number of cookies captured, or 0 when the site has not been signed in.
     */
    fun captureFromWebView(context: Context, url: String): Int {
        val manager = CookieManager.getInstance()
        manager.setAcceptCookie(true)
        val host = runCatching { Uri.parse(url).host }.getOrNull()
            ?.removePrefix(".")
            ?.takeIf { it.isNotBlank() }
            ?: return 0

        val raw = runCatching { manager.getCookie(url) }.getOrNull().orEmpty()
        if (raw.isBlank()) return 0

        val expiry = System.currentTimeMillis() / 1000L + HALF_YEAR_SECONDS
        val rows = raw.split(';').mapNotNull { pair ->
            val index = pair.indexOf('=')
            if (index <= 0) return@mapNotNull null
            val name = pair.substring(0, index).trim()
            val value = pair.substring(index + 1).trim()
            if (name.isBlank()) return@mapNotNull null
            "$host\tTRUE\t/\tTRUE\t$expiry\t$name\t$value"
        }
        if (rows.isEmpty()) return 0

        val header = buildString {
            appendLine("# Netscape HTTP Cookie File")
            appendLine("# Captured inside VidVibe's secure sign-in window.")
            appendLine()
        }
        runCatching {
            cookiesFile(context).writeText(header + rows.joinToString("\n") + "\n")
        }.getOrElse { return 0 }
        return rows.size
    }

    /**
     * Imports a cookies.txt that the user dropped into Downloads (or the app's
     * own external folder). Only runs when VidVibe does not already have one.
     */
    @Suppress("DEPRECATION")
    fun autoImport(context: Context): Boolean {
        if (hasCookies(context)) return false

        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val candidates = listOf(
            File(downloads, "cookies.txt"),
            File(downloads, "VidVibe/cookies.txt"),
            File(context.getExternalFilesDir(null) ?: context.filesDir, "cookies.txt")
        )

        val source = candidates.firstOrNull {
            it.isFile && it.length() > 0L && looksLikeCookies(it)
        } ?: return false

        return runCatching {
            source.inputStream().use { input ->
                cookiesFile(context).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            hasCookies(context)
        }.getOrDefault(false)
    }

    fun clear(context: Context) {
        runCatching { cookiesFile(context).delete() }
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }
}
