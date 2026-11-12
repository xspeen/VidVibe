import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.File
import java.net.URI
import java.util.zip.ZipInputStream

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

kotlin {
    jvmToolchain(17)
}

val toolsDir = File(layout.buildDirectory.get().asFile, "tools")

fun downloadFile(url: String, target: File) {
    target.parentFile?.mkdirs()
    var current = URI(url).toURL()
    var redirects = 0
    while (redirects < 8) {
        val connection = current.openConnection()
        connection.setRequestProperty("User-Agent", "VidVibe-Build")
        connection.connectTimeout = 30_000
        connection.readTimeout = 120_000
        val code = (connection as? java.net.HttpURLConnection)?.responseCode ?: 200
        if (code in 300..399) {
            val location = connection.getHeaderField("Location") ?: break
            current = current.toURI().resolve(location).toURL()
            redirects++
            continue
        }
        connection.getInputStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return
    }
    error("Could not download $url")
}

fun extractFromZip(zip: File, names: List<String>, destination: File) {
    ZipInputStream(zip.inputStream().buffered()).use { stream ->
        while (true) {
            val entry = stream.nextEntry ?: break
            val name = entry.name.substringAfterLast('/')
            if (!entry.isDirectory && name in names) {
                File(destination, name).outputStream().use { output ->
                    stream.copyTo(output)
                }
            }
            stream.closeEntry()
        }
    }
}

val downloadTools by tasks.registering {
    description = "Fetches yt-dlp.exe and ffmpeg.exe so the installer works offline."
    outputs.dir(toolsDir)
    doLast {
        toolsDir.mkdirs()
        val ytDlp = File(toolsDir, "yt-dlp.exe")
        if (!ytDlp.isFile || ytDlp.length() < 1_000_000L) {
            println("Downloading yt-dlp.exe")
            downloadFile(
                "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe",
                ytDlp
            )
        }
        val ffmpeg = File(toolsDir, "ffmpeg.exe")
        if (!ffmpeg.isFile || ffmpeg.length() < 1_000_000L) {
            println("Downloading ffmpeg")
            val zip = File(toolsDir, "ffmpeg.zip")
            downloadFile(
                "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip",
                zip
            )
            extractFromZip(zip, listOf("ffmpeg.exe", "ffprobe.exe"), toolsDir)
            zip.delete()
        }
        check(ytDlp.length() > 1_000_000L) { "yt-dlp.exe download failed" }
        check(ffmpeg.length() > 1_000_000L) { "ffmpeg.exe download failed" }
    }
}

sourceSets["main"].resources.srcDir(toolsDir)
tasks.named("processResources") { dependsOn(downloadTools) }

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.vidvibe.desktop.MainKt"

        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "VidVibe"
            packageVersion = "2.8.3"
            description = "VidVibe video downloader for Windows"
            vendor = "VidVibe"
            copyright = "MIT License"

            windows {
                menuGroup = "VidVibe"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8f1c2b76-3a1e-4a8e-9a52-4a0d3b7c6e11"
            }
        }
    }
}
