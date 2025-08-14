package com.efedonmez.nothingmatrixmusicdisc.toy

import java.util.concurrent.atomic.AtomicReference

data class PreviewFrame(val width: Int, val height: Int, val pixels: IntArray)

object GlyphPreviewStore {
    private val latestRef = AtomicReference<PreviewFrame?>(null)

    fun update(width: Int, height: Int, pixels: IntArray) {
        // Defensive copy to avoid accidental mutation
        val safe = pixels.copyOf()
        latestRef.set(PreviewFrame(width, height, safe))
    }

    fun get(): PreviewFrame? = latestRef.get()
}


