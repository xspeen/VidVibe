"""VidVibe downloader bridge.

Modern sites (YouTube in particular) only publish separate video-only and
audio-only streams. Android has no bundled FFmpeg, so this module downloads the
two streams separately and lets the Kotlin side mux them with MediaMuxer.

When a site still offers a progressive (video+audio) file, that file is used
directly instead. A muxed fallback is also available for cases where the
on-device merge cannot handle the downloaded codecs.
"""

import json
import os
import re
import shutil
import time
import uuid

import yt_dlp


VIDEO_EXTENSIONS = (".mp4", ".m4v", ".mov", ".webm", ".mkv", ".3gp")
AUDIO_EXTENSIONS = (".m4a", ".mp4", ".aac", ".mp3", ".opus", ".webm")


def _notify(callback, percent, status):
    if callback is None:
        return
    try:
        callback.onProgress(int(max(0, min(100, percent))), status)
    except Exception:
        # A UI callback must never abort an active download.
        pass


def _video_selector(max_height):
    """Prefer H.264 MP4 because MediaMuxer can always mux it into MP4."""
    selectors = []
    for codec in ("[vcodec^=avc1]", ""):
        base = "bv*[acodec=none][ext=mp4]" + codec
        if max_height and max_height > 0:
            selectors.append("%s[height<=%d]" % (base, max_height))
        selectors.append(base)
    base = "bv*[acodec=none]"
    if max_height and max_height > 0:
        selectors.append("%s[height<=%d]" % (base, max_height))
    selectors.append(base)
    return "/".join(selectors)


def _audio_selector():
    # MediaMuxer writes AAC into MP4 reliably; other codecs fall back to muxed.
    return (
        "ba[acodec!=none][ext=m4a]/"
        "ba[acodec!=none][ext=mp4]/"
        "ba[acodec!=none][acodec^=mp4a]"
    )


def _muxed_selector(max_height):
    selectors = []
    for codec in ("[vcodec^=avc1]", ""):
        base = "b[acodec!=none][vcodec!=none][ext=mp4]" + codec
        if max_height and max_height > 0:
            selectors.append("%s[height<=%d]" % (base, max_height))
        selectors.append(base)
    base = "b[acodec!=none][vcodec!=none]"
    if max_height and max_height > 0:
        selectors.append("%s[height<=%d]" % (base, max_height))
    selectors.append(base)
    selectors.append("b")
    return "/".join(selectors)


def _base_options(output_dir, progress_hook, extractor_args=None, cookies_path=None):
    options = {
        "quiet": True,
        "no_warnings": True,
        "no_color": True,
        "noprogress": True,
        "noplaylist": True,
        "restrictfilenames": True,
        "overwrites": True,
        "retries": 5,
        "fragment_retries": 5,
        "extractor_retries": 3,
        "file_access_retries": 3,
        "socket_timeout": 30,
        "concurrent_fragment_downloads": 4,
        "cachedir": os.path.join(output_dir, ".yt-dlp-cache"),
        "progress_hooks": [progress_hook] if progress_hook else [],
        "fixup": "never",
        "nopostoverwrites": True,
        "continuedl": True,
        "nocheckcertificate": True,
        "geo_bypass": True,
        "ignoreerrors": False,
    }
    if extractor_args:
        options["extractor_args"] = extractor_args
    # A cookies.txt exported from a signed-in browser is the only way to reach
    # private / login-only posts, so use it whenever the user imported one.
    if cookies_path and os.path.isfile(cookies_path):
        options["cookiefile"] = cookies_path
    return options


def _safe_name(value, fallback="video"):
    cleaned = re.sub(r"[^A-Za-z0-9._ -]+", "_", str(value or "")).strip(" ._-")
    cleaned = re.sub(r"\s+", " ", cleaned)
    return (cleaned[:70] or fallback)


def _first_entry(info):
    if not info:
        return None
    if info.get("_type") != "playlist":
        return info
    entries = [entry for entry in (info.get("entries") or []) if entry]
    return entries[0] if entries else None


def _downloaded_path(info, job_dir, prefix):
    if info:
        for requested in info.get("requested_downloads") or []:
            path = requested.get("filepath") or requested.get("filename")
            if path and os.path.isfile(path):
                return os.path.abspath(path)

    candidates = []
    for name in os.listdir(job_dir):
        if not name.startswith(prefix):
            continue
        if name.endswith((".part", ".ytdl", ".temp")):
            continue
        path = os.path.join(job_dir, name)
        if os.path.isfile(path) and os.path.getsize(path) > 0:
            candidates.append(path)
    if not candidates:
        return None
    return os.path.abspath(max(candidates, key=os.path.getmtime))


def _download_stream(
    url,
    selector,
    job_dir,
    prefix,
    output_dir,
    progress_callback,
    start_percent,
    end_percent,
    status_text,
    extractor_args=None,
    cookies_path=None,
):
    span = max(1, end_percent - start_percent)
    last_update = [0.0]
    last_percent = [-1]

    def progress_hook(data):
        status = data.get("status")
        if status == "downloading":
            total = data.get("total_bytes") or data.get("total_bytes_estimate") or 0
            downloaded = data.get("downloaded_bytes") or 0
            fraction = (downloaded / total) if total else 0.0
            percent = int(start_percent + fraction * span)
            now = time.time()
            if percent != last_percent[0] or now - last_update[0] > 0.35:
                last_update[0] = now
                last_percent[0] = percent
                _notify(progress_callback, min(percent, end_percent), status_text)
        elif status == "finished":
            _notify(progress_callback, end_percent, status_text)

    options = _base_options(output_dir, progress_hook, extractor_args, cookies_path)
    options["format"] = selector
    options["outtmpl"] = os.path.join(job_dir, prefix + ".%(ext)s")

    with yt_dlp.YoutubeDL(options) as downloader:
        info = downloader.extract_info(url, download=True)
        info = _first_entry(info)
        path = _downloaded_path(info, job_dir, prefix)
        if not path:
            raise RuntimeError("The downloaded stream could not be located.")
        return path, info


def _cleanup_job(job_dir):
    run_catching_delete = getattr(shutil, "rmtree", None)
    if run_catching_delete is None:
        return
    try:
        shutil.rmtree(job_dir, ignore_errors=True)
    except Exception:
        pass


def _friendly_error(url, error):
    host = ""
    try:
        from urllib.parse import urlparse

        host = (urlparse(url).hostname or "").lower()
    except Exception:
        pass

    detail = str(error or "").strip()
    first_line = detail.splitlines()[0] if detail else ""
    lower = detail.lower()

    needs_login = any(
        token in lower
        for token in (
            "login required",
            "log in",
            "sign in",
            "logged-in",
            "private",
            "premium",
            "only available for registered users",
            "rate-limit reached",
            "requested content is not available",
            "authentication",
            "cookies",
            "account",
        )
    )

    if "instagram.com" in host:
        if needs_login:
            return (
                "Instagram blocked this post because it needs a signed-in account. "
                "Export cookies.txt from a browser where you are logged in, import it "
                "in Profile > App Settings, then try again."
            )
        return (
            "Instagram did not return this video. Public reels and posts work; private "
            "accounts need cookies.txt imported in Profile > App Settings."
        )
    if "pornhub" in host:
        if needs_login:
            return (
                "Pornhub asked for a signed-in account for this video. Import a "
                "cookies.txt exported from a logged-in browser in Profile > App Settings."
            )
        return (
            "Pornhub did not return this video. Try the direct watch link; age-gated or "
            "premium videos need cookies.txt."
        )
    if "youtube.com" in host or "youtu.be" in host:
        return (
            "YouTube did not provide a downloadable stream. The video may be private, "
            "age-restricted, or region-locked. Try another public link."
        )
    if "tiktok.com" in host:
        return (
            "TikTok did not provide this video. Make sure the post is public and the "
            "link opens in a browser."
        )
    if "facebook.com" in host or "fb.watch" in host:
        return (
            "Facebook did not provide this video. Public posts work best; private or "
            "group posts need cookies.txt."
        )
    if needs_login:
        return (
            "This site asked for a signed-in account. Import a cookies.txt exported "
            "from a logged-in browser in Profile > App Settings."
        )
    if first_line:
        return "This link could not be downloaded. %s" % first_line[:220]
    return "This link could not be downloaded. The site may have changed or blocked the request."


def _merge_result(job_dir, video_path, audio_path, info):
    info = info or {}
    title = _safe_name(info.get("title"), "video")
    video_id = _safe_name(info.get("id"), str(int(time.time())))
    return {
        "mode": "merge",
        "video_path": video_path,
        "audio_path": audio_path,
        "file_path": None,
        "job_dir": job_dir,
        "output_name": "%s-%s.mp4" % (title, video_id),
        "title": info.get("title") or title,
        "id": info.get("id") or video_id,
        "ext": "mp4",
        "height": info.get("height") or 0,
    }


def _single_result(job_dir, path, info):
    info = info or {}
    extension = os.path.splitext(path)[1].lstrip(".").lower() or "mp4"
    title = _safe_name(info.get("title"), "video")
    video_id = _safe_name(info.get("id"), str(int(time.time())))
    return {
        "mode": "single",
        "video_path": None,
        "audio_path": None,
        "file_path": path,
        "job_dir": job_dir,
        "output_name": "%s-%s.%s" % (title, video_id, extension),
        "title": info.get("title") or title,
        "id": info.get("id") or video_id,
        "ext": extension,
        "height": info.get("height") or 0,
    }


def _try_split(url, max_height, job_dir, output_dir, progress_callback, cookies_path=None):
    _notify(progress_callback, 1, "Finding the best video stream...")
    video_path, info = _download_stream(
        url,
        _video_selector(max_height),
        job_dir,
        "video",
        output_dir,
        progress_callback,
        2,
        82,
        "Downloading video...",
        cookies_path=cookies_path,
    )

    _notify(progress_callback, 83, "Downloading audio...")
    audio_path, _ = _download_stream(
        url,
        _audio_selector(),
        job_dir,
        "audio",
        output_dir,
        progress_callback,
        83,
        97,
        "Downloading audio...",
        cookies_path=cookies_path,
    )
    return _merge_result(job_dir, video_path, audio_path, info)


def _try_muxed(
    url,
    max_height,
    job_dir,
    output_dir,
    progress_callback,
    extractor_args=None,
    start_percent=1,
    end_percent=97,
    label="Downloading video...",
    cookies_path=None,
):
    _notify(progress_callback, start_percent, label)
    path, info = _download_stream(
        url,
        _muxed_selector(max_height),
        job_dir,
        "muxed",
        output_dir,
        progress_callback,
        start_percent,
        end_percent,
        label,
        extractor_args=extractor_args,
        cookies_path=cookies_path,
    )
    return _single_result(job_dir, path, info)


def download_video(url, quality_height, output_directory, progress_callback, cookies_path=""):
    """Called from Kotlin through Chaquopy.

    Returns a JSON string describing either a merge job (video + audio paths)
    or a single progressive file.
    """
    clean_url = str(url or "").strip()
    if not clean_url:
        raise ValueError("Paste a link before starting a download.")

    os.makedirs(output_directory, exist_ok=True)
    job_dir = os.path.join(output_directory, ".job-" + uuid.uuid4().hex)
    os.makedirs(job_dir, exist_ok=True)
    max_height = int(quality_height or 0)

    _notify(progress_callback, 0, "Preparing download...")

    split_error = None
    try:
        result = _try_split(
            clean_url,
            max_height,
            job_dir,
            output_directory,
            progress_callback,
            cookies_path=cookies_path,
        )
        _notify(progress_callback, 98, "Merging video and audio...")
        return json.dumps(result)
    except Exception as error:
        split_error = error
        # Remove any partial split streams before trying a progressive file.
        for name in os.listdir(job_dir):
            if name.startswith(("video", "audio")):
                run_catching = os.path.join(job_dir, name)
                try:
                    os.remove(run_catching)
                except Exception:
                    pass

    # Progressive (video+audio) fallback with the default clients.
    _notify(progress_callback, 1, "Trying a single-stream download...")
    try:
        result = _try_muxed(
            clean_url,
            max_height,
            job_dir,
            output_directory,
            progress_callback,
            cookies_path=cookies_path,
        )
        return json.dumps(result)
    except Exception:
        pass

    # Some YouTube videos only expose a progressive stream through the
    # Android player client.
    try:
        result = _try_muxed(
            clean_url,
            max_height,
            job_dir,
            output_directory,
            progress_callback,
            extractor_args={"youtube": {"player_client": ["android", "android_vr"]}},
            start_percent=1,
            end_percent=97,
            label="Trying the mobile stream...",
            cookies_path=cookies_path,
        )
        return json.dumps(result)
    except Exception:
        pass

    # Last resort: let yt-dlp pick anything usable, without a quality ceiling.
    try:
        result = _try_muxed(
            clean_url,
            0,
            job_dir,
            output_directory,
            progress_callback,
            start_percent=1,
            end_percent=97,
            label="Trying the best available stream...",
            cookies_path=cookies_path,
        )
        return json.dumps(result)
    except Exception:
        _cleanup_job(job_dir)
        raise RuntimeError(_friendly_error(clean_url, split_error))


def download_fallback(url, quality_height, output_directory, progress_callback, cookies_path=""):
    """Muxed-only fallback used when the on-device merge cannot run."""
    clean_url = str(url or "").strip()
    if not clean_url:
        raise ValueError("Paste a link before starting a download.")

    os.makedirs(output_directory, exist_ok=True)
    job_dir = os.path.join(output_directory, ".job-" + uuid.uuid4().hex)
    os.makedirs(job_dir, exist_ok=True)
    max_height = int(quality_height or 0)

    _notify(progress_callback, 1, "Downloading a single stream...")
    last_error = None
    for extractor_args in (
        None,
        {"youtube": {"player_client": ["android", "android_vr"]}},
    ):
        try:
            result = _try_muxed(
                clean_url,
                max_height,
                job_dir,
                output_directory,
                progress_callback,
                extractor_args=extractor_args,
                start_percent=1,
                end_percent=98,
                label="Downloading video...",
                cookies_path=cookies_path,
            )
            return json.dumps(result)
        except Exception as error:
            last_error = error
            for name in os.listdir(job_dir):
                try:
                    os.remove(os.path.join(job_dir, name))
                except Exception:
                    pass

    try:
        result = _try_muxed(
            clean_url,
            0,
            job_dir,
            output_directory,
            progress_callback,
            start_percent=1,
            end_percent=98,
            label="Downloading video...",
            cookies_path=cookies_path,
        )
        return json.dumps(result)
    except Exception:
        _cleanup_job(job_dir)
        raise RuntimeError(_friendly_error(clean_url, last_error))
