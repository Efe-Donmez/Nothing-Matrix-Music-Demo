package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.app.Notification
import android.graphics.Bitmap
import android.os.Bundle
import java.util.concurrent.atomic.AtomicReference

object NowPlayingStore {

    private val latestTextRef = AtomicReference<String?>(null)
    private val latestInfoRef = AtomicReference<NowPlayingInfo?>(null)

    fun update(title: String?, artist: String?, album: String? = null, art: Bitmap? = null, isPlaying: Boolean = true, posMs: Long? = null, durMs: Long? = null, pkg: String? = null) {
        val t = title?.trim().orEmpty()
        val a = artist?.trim().orEmpty()
        val composed = when {
            t.isNotEmpty() && a.isNotEmpty() -> "$a - $t"
            t.isNotEmpty() -> t
            a.isNotEmpty() -> a
            else -> null
        }
        latestTextRef.set(composed)
        latestInfoRef.set(NowPlayingInfo(title = title, artist = artist, album = album, isPlaying = isPlaying, positionMs = posMs, durationMs = durMs, art = art, sourcePackage = pkg))
    }

    fun updateFromSingleLine(line: String?, pkg: String? = null) {
        val text = line?.trim()
        if (!text.isNullOrEmpty()) {
            latestTextRef.set(text)
            latestInfoRef.set(NowPlayingInfo(title = text, artist = null, album = null, isPlaying = true, positionMs = null, durationMs = null, art = null, sourcePackage = pkg))
        }
    }

    fun getText(): String? = latestTextRef.get()
    fun getInfo(): NowPlayingInfo? = latestInfoRef.get()
}


