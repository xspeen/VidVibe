package com.vidvibe.app

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.chaquo.python.android.PyApplication

class VidVibeApplication : PyApplication(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(180)
            .build()
    }
}
