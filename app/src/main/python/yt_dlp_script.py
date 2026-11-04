"""VidVibe downloader bridge.

This module is executed in the embedded Chaquopy Python runtime. It deliberately
asks yt-dlp for a single progressive file which already contains both video and
audio, because a normal Android app does not ship the FFmpeg binary required to
merge separate DASH streams.
"""

import os
import time

import yt_dlp


def _notify(callback, percent, status):
    if callback is None:
        return
    try:
        callback.onProgress(int(max(0, min(100, percent))), status)
    except Exception:
        # A UI callback must never abort an active download.
        pass


def _quality_format(max_height):
    if max_height and max_height > 0:
        return (
            "best[height<=%d][ext=mp4][vcodec!=none][acodec!=none]/"
            "best[height<=%d][vcodec!=none][acodec!=none]/"
            "best[ext=mp4][vcodec!=none][acodec!=none]/"
            "best[ext=mp4]/best" % (max_height, max_height)
        )
    return "best[ext=mp4][vcodec!=none][acodec!=none]/best[ext=mp4]/best"


def _download(url, max_height, output_dir, progress_callback):
    last_update = [0.0]
    last_percent = [-1]

    def progress_hook(data):
        status = data.get("status")
        if status == "downloading":
            total = data.get("total_bytes") or data.get("total_bytes_estimate") or 0
            downloaded = data.get("downloaded_bytes") or 0
            if total:
                percent = int(downloaded * 100 / total)
            else:
                percent = 0

            now = time.time()
            if percent != last_percent[0] or now - last_update[0] > 0.35:
                last_update[0] = now
                last_percent[0] = percent
                _notify(progress_callback, min(percent, 99), "Downloading video...")
        elif status == "finished":
            _notify(progress_callback, 99, "Finishing download...")

    output_template = os.path.join(
        output_dir,
        "%(title).70s-%(id)s.%(ext)s"
    )

    options = {
        "format": _quality_format(max_height),
        "outtmpl": output_template,
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "restrictfilenames": True,
        "overwrites": True,
        "retries": 3,
        "fragment_retries": 3,
        "socket_timeout": 30,
        "concurrent_fragment_downloads": 3,
        "cachedir": os.path.join(output_dir, ".yt-dlp-cache"),
        "progress_hooks": [progress_hook],
        "http_headers": {
            "User-Agent": (
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
                "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            )
        },
    }

    with yt_dlp.YoutubeDL(options) as downloader:
        info = downloader.extract_info(url, download=True)
        if info is None:
            raise RuntimeError("No downloadable media was found at this link.")

        if info.get("_type") == "playlist":
            entries = [entry for entry in (info.get("entries") or []) if entry]
            if not entries:
                raise RuntimeError("The link did not contain a downloadable video.")
            info = entries[0]

        candidates = []
        for requested in info.get("requested_downloads") or []:
            path = requested.get("filepath") or requested.get("filename")
            if path:
                candidates.append(path)
        prepared = downloader.prepare_filename(info)
        if prepared:
            candidates.append(prepared)

        for candidate in candidates:
            if os.path.isfile(candidate) and os.path.getsize(candidate) > 0:
                _notify(progress_callback, 100, "Saving to gallery...")
                return os.path.abspath(candidate)

        # Last-resort recovery: pick the newest completed video in the output
        # directory. This covers extractors which rewrite the final extension.
        video_extensions = (".mp4", ".m4v", ".webm", ".mkv", ".mov", ".3gp")
        completed = []
        for name in os.listdir(output_dir):
            candidate = os.path.join(output_dir, name)
            if (
                os.path.isfile(candidate)
                and name.lower().endswith(video_extensions)
                and not name.endswith(".part")
                and os.path.getsize(candidate) > 0
            ):
                completed.append(candidate)
        if completed:
            newest = max(completed, key=lambda item: os.path.getmtime(item))
            _notify(progress_callback, 100, "Saving to gallery...")
            return os.path.abspath(newest)

        raise RuntimeError("The video was downloaded, but the output file could not be located.")


def download_video(url, quality_height, output_directory, progress_callback):
    """Called from Kotlin through Chaquopy.

    Returns the absolute path of the downloaded progressive video file.
    """
    clean_url = str(url or "").strip()
    if not clean_url:
        raise ValueError("Paste a link before starting a download.")

    os.makedirs(output_directory, exist_ok=True)
    _notify(progress_callback, 0, "Preparing download...")

    try:
        return _download(
            clean_url,
            int(quality_height or 0),
            output_directory,
            progress_callback,
        )
    except yt_dlp.utils.DownloadError as first_error:
        # Some extractors reject a height filter but still expose a usable
        # progressive stream. Retry once without forcing a quality ceiling.
        _notify(progress_callback, 0, "Retrying with the best available quality...")
        try:
            return _download(clean_url, 0, output_directory, progress_callback)
        except Exception:
            raise first_error
