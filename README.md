# VidVibe

VidVibe is a free, open source Android video library and social video downloader.

It brings the videos already on your device into a fast, dark, carefully animated
library, and it can download a video from a public link straight into your gallery.
There are no accounts, no ads, and no analytics. Your media stays on your phone.

[![Android CI](https://github.com/xspeen/VidVibe/actions/workflows/android-ci.yml/badge.svg)](https://github.com/xspeen/VidVibe/actions/workflows/android-ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Features

### Local library
- Reads videos from the device through MediaStore.
- Videos, Folders, and Playlists tabs.
- Search, grid and list layouts, and shuffle.
- Custom pull-to-refresh with a twelve-blade spinner.
- Long-press selection with a floating Share, Add to, and Delete menu.
- Playlist creation, add-to-playlist, and playlist removal.
- Video playback with Media3 ExoPlayer.
- Permanent deletion through MediaStore, including the Android system
  confirmation flow where the platform requires it.

### Downloader
- Paste a public link and download a video into `Movies/VidVibe`.
- Full-screen quality selection from 1080p down to 360p, with automatic fallback
  to the best available progressive stream.
- Full-screen circular progress page with a live percentage.
- Success and error states with retry.
- Wi-Fi-only mode.
- Powered by yt-dlp running in an embedded Python runtime through Chaquopy.

### Me
- Global font size slider from 12sp to 24sp.
- Liquid-glass style navigation toggle.
- Six app background colors.
- Download settings, including Wi-Fi-only mode.
- About, source code, and license pages.

### Privacy and safety
- `FLAG_SECURE` is enabled on the main window, which blocks screenshots, screen
  recording, and the recent-apps thumbnail.
- No INTERNET use except the downloader. The local library is read directly from
  MediaStore.
- No accounts, advertising SDKs, or analytics.
- Temporary downloaded files are removed after they are published to the gallery.

## Screens

| Local | Downloader | Me |
| --- | --- | --- |
| Videos, folders, playlists, search, selection | Paste, quality, progress, save | Font size, glass toggle, background, downloads |

## Tech stack

- Kotlin 2.0.21
- Jetpack Compose with Material 3
- Media3 ExoPlayer 1.5.1
- Coil with `coil-video` for thumbnails
- DataStore Preferences for settings
- SharedPreferences JSON storage for playlists
- Chaquopy 16.1.0 with Python 3.12
- yt-dlp 2025.09.05
- AGP 8.7.3, Gradle 8.9, compileSdk and targetSdk 35, minSdk 24

## Requirements

- JDK 17 for the Android build.
- Python 3.12 available on the build machine. Chaquopy uses it to resolve and
  package the embedded Python runtime and pip packages.
- Android SDK Platform 35 and Build Tools for `compileSdk` 35.

GitHub Actions installs all of these automatically.

## Build with GitHub Actions

No local Android SDK is required. The workflows in `.github/workflows` build the
app in the cloud.

### Debug builds

`.github/workflows/android-ci.yml` runs on every push to `main`, every pull
request, and manual dispatch. It builds `app-debug.apk` and uploads it as a
workflow artifact.

### Release builds

`.github/workflows/android-release.yml` runs when a tag beginning with `v` is
pushed. It builds the release APK, builds a debug APK as a fallback, and publishes
a GitHub Release with the APKs attached.

```bash
git tag v1.0.0
git push origin v1.0.0
```

Manual dispatch also builds the APKs and uploads them as artifacts, but does not
publish a GitHub Release.

### Release signing

For a signed release, add these repository secrets:

| Secret | Meaning |
| --- | --- |
| `VIDVIBE_KEYSTORE_BASE64` | Base64-encoded release keystore |
| `VIDVIBE_KEYSTORE_PASSWORD` | Keystore password |
| `VIDVIBE_KEY_ALIAS` | Key alias |
| `VIDVIBE_KEY_PASSWORD` | Key password |

Create a keystore and encode it:

```bash
keytool -genkeypair -v \
  -keystore vidvibe-release.jks \
  -alias vidvibe \
  -keyalg RSA -keysize 4096 -validity 10000
base64 -w 0 vidvibe-release.jks > vidvibe-release.jks.base64
```

Paste the contents of `vidvibe-release.jks.base64` into
`VIDVIBE_KEYSTORE_BASE64`. Never commit the keystore or its passwords.

If the secrets are not configured, the release workflow produces an unsigned
release APK plus an installable debug APK.

## Build locally (optional)

```bash
# JDK 17 and Python 3.12 are required
export JAVA_HOME=/path/to/jdk-17
export PATH="/path/to/python3.12/bin:$PATH"

# Point Gradle at the Android SDK
echo "sdk.dir=/path/to/android-sdk" > local.properties

./gradlew :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If a Gradle wrapper is not present, run `gradle :app:assembleDebug` with Gradle
8.9 installed.

## Project structure

```text
app/src/main/java/com/vidvibe/app/
  MainActivity.kt              FLAG_SECURE, permissions, theme entry
  VidVibeApplication.kt        Chaquopy application and Coil video decoding
  data/
    VideoItem.kt               Media model and formatting
    VideoRepository.kt         MediaStore query, permanent delete, gallery save
    SettingsStore.kt           DataStore-backed settings
    PlaylistStore.kt           Playlist persistence
  download/
    YtDlpManager.kt            Kotlin to Python download bridge
  ui/
    VidVibeApp.kt              App shell, navigation, dialogs, player overlay
    components/                Bottom nav, drawer, loader, delete sheet, refresh
    screens/                   Local, Downloader, Me, video player
    theme/                     Colors, typography, font scaling, background
    viewmodel/                 State, downloads, settings, playlists
app/src/main/python/
  yt_dlp_script.py             yt-dlp progress bridge and progressive format choice
```

## Permissions

- `READ_MEDIA_VIDEO` on Android 13 and newer.
- `READ_EXTERNAL_STORAGE` on older releases.
- `WRITE_EXTERNAL_STORAGE` only on Android 9 and older, for gallery saves.
- `INTERNET` and `ACCESS_NETWORK_STATE` for the downloader.

VidVibe never uploads your library. The only outbound traffic is the download you
explicitly start.

## Downloader notes

VidVibe intentionally selects progressive MP4 streams which already contain both
video and audio. This avoids shipping FFmpeg just to merge separate DASH streams,
which keeps the APK small. If the requested quality has no progressive stream,
the downloader retries with the best available quality.

Only download media you have the right to save. Respect platform terms and
copyright law.

## Deleting videos

Videos are removed permanently from physical storage through `ContentResolver`.
On Android 11 and newer, the system may show a confirmation dialog for media the
app does not own. On Android 10, `RecoverableSecurityException` is handled. There
is no in-app recycle bin because deletion is intentionally permanent.

## Contributing

1. Fork the repository.
2. Create a branch.
3. Make your change.
4. Open a pull request. The CI workflow builds a debug APK for every pull request.

## License

MIT License. See [LICENSE](LICENSE).
