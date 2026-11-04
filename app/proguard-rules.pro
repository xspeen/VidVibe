# Chaquopy
-keep class com.chaquo.python.** { *; }
-keep class com.vidvibe.app.download.DownloadProgressCallback { *; }

# Keep callback method names used from Python.
-keepclassmembers class * implements com.vidvibe.app.download.DownloadProgressCallback {
    public void onProgress(int, java.lang.String);
}

# Media3
-dontwarn androidx.media3.**
