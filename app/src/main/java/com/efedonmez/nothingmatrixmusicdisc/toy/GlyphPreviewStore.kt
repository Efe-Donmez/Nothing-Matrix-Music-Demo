package com.efedonmez.nothingmatrixmusicdisc.toy

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicReference

data class PreviewFrame(val width: Int, val height: Int, val pixels: IntArray)

object GlyphPreviewStore {
    private val latestRef = AtomicReference<PreviewFrame?>(null)
    private val handler = Handler(Looper.getMainLooper())
    private var pendingClear: Runnable? = null

    fun update(width: Int, height: Int, pixels: IntArray) {
        // Defensive copy to avoid accidental mutation
        val safe = pixels.copyOf()
        latestRef.set(PreviewFrame(width, height, safe))
    }

    fun get(): PreviewFrame? = latestRef.get()

    fun clearNow() {
        latestRef.set(null)
    }

    fun scheduleFullClear(delayMs: Long) {
        pendingClear?.let { handler.removeCallbacks(it) }
        val r = Runnable { clearNow() }
        pendingClear = r
        handler.postDelayed(r, delayMs.coerceAtLeast(0))
    }
}


