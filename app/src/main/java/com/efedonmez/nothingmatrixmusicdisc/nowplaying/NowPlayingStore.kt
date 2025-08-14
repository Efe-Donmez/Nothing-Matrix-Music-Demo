package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.app.Notification
import android.graphics.Bitmap
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference

object NowPlayingStore {

    private val latestTextRef = AtomicReference<String?>(null)
    private val latestInfoRef = AtomicReference<NowPlayingInfo?>(null)
    private val infoFlowInternal = MutableStateFlow<NowPlayingInfo?>(null)
    private val textFlowInternal = MutableStateFlow<String?>(null)

    fun update(title: String?, artist: String?, album: String? = null, art: Bitmap? = null, isPlaying: Boolean = true, posMs: Long? = null, durMs: Long? = null, pkg: String? = null) {
        val t = title?.trim().orEmpty()
        val a = artist?.trim().orEmpty()
        val composed = when {
            t.isNotEmpty() && a.isNotEmpty() -> "$a - $t"
            t.isNotEmpty() -> t
            a.isNotEmpty() -> a
            else -> null
        }
        val info = NowPlayingInfo(title = title, artist = artist, album = album, isPlaying = isPlaying, positionMs = posMs, durationMs = durMs, art = art, sourcePackage = pkg)
        latestTextRef.set(composed)
        latestInfoRef.set(info)
        infoFlowInternal.value = info
        textFlowInternal.value = composed
    }

    fun updateFromSingleLine(line: String?, pkg: String? = null) {
        val text = line?.trim()
        if (!text.isNullOrEmpty()) {
            val info = NowPlayingInfo(title = text, artist = null, album = null, isPlaying = true, positionMs = null, durationMs = null, art = null, sourcePackage = pkg)
            latestTextRef.set(text)
            latestInfoRef.set(info)
            infoFlowInternal.value = info
            textFlowInternal.value = text
        }
    }

    fun getText(): String? = latestTextRef.get()
    fun getInfo(): NowPlayingInfo? = latestInfoRef.get()
    fun infoFlow(): StateFlow<NowPlayingInfo?> = infoFlowInternal
    fun textFlow(): StateFlow<String?> = textFlowInternal
}


