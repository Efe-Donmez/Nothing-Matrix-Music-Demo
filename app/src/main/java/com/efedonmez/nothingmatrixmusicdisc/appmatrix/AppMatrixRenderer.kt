package com.efedonmez.nothingmatrixmusicdisc.appmatrix

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.efedonmez.nothingmatrixmusicdisc.util.TextSanitizer

object AppMatrixRenderer {

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var manager: GlyphMatrixManager? = null
    @Volatile private var isInitialized: Boolean = false
    @Volatile private var pendingText: String? = null
    @Volatile private var isAnimating: Boolean = false
    private var lastText: String = ""
    private var lastWidth: Int = 40
    private var scrollX: Int = 25
    private val entryPadding = 12
    private val resetGap = 0
    private val loopPadding = 0
    private var loopCount: Int = 0

    fun renderText(context: Context, text: String) {
        val safeText = text.takeIf { it.isNotBlank() } ?: return
        val sanitized = TextSanitizer.sanitizeForGlyph(safeText)
        if (isInitialized && manager != null) {
            startOrUpdateMarquee(context, sanitized)
            return
        }
        // Not initialized yet: store and init
        pendingText = sanitized
        ensureInit(context)
    }

    private fun ensureInit(context: Context) {
        if (manager != null) return
        val m = GlyphMatrixManager.getInstance(context.applicationContext)
        manager = m
        m.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName) {
                // Register device; adjust if needed
                m.register(Glyph.DEVICE_23111)
                isInitialized = true
                pendingText?.let { text ->
                    pendingText = null
                    startOrUpdateMarquee(context, text)
                }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                isInitialized = false
            }
        })
    }

    private fun startOrUpdateMarquee(context: Context, text: String) {
        val t = text.ifBlank { return }
        
        // 🛑 Önceki animasyonu tamamen durdur
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
        
        // 🔄 Yeni animasyonu başlat  
        lastText = t
        lastWidth = estimateTextWidthCells(lastText)
        scrollX = 25 + entryPadding
        loopCount = 0
        isAnimating = true
        handler.post(animateRunnable(context))
    }

    private fun animateRunnable(context: Context): Runnable = object : Runnable {
        override fun run() {
            if (!isAnimating) return
            val m = manager ?: return

            val obj = GlyphMatrixObject.Builder()
                .setText(lastText)
                .setPosition(scrollX, 10)
                .setScale(100)
                .setBrightness(255)
                .build()

            val frame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(context)

            m.setAppMatrixFrame(frame)

            scrollX -= 1
            val endX = -lastWidth - resetGap
            if (scrollX < endX) {
                // Bir tur tamamlandı
                loopCount += 1
                if (loopCount >= 2) {
                    // İki tur tamamlandı: temizle ve kapat
                    isAnimating = false
                    handler.removeCallbacksAndMessages(null)
                    AppMatrixControl.closeAfter(context, 100) // Hemen kapat
                    return
                } else {
                    // Yeni tura başla
                    scrollX = 25 + loopPadding
                }
            }

            val delay = normalDelayMsFor(lastText).toLong()
            handler.postDelayed(this, delay)
        }
    }

    private fun estimateTextWidthCells(text: String): Int {
        return (text.length * 6).coerceAtLeast(8).coerceAtMost(120)
    }

    private fun normalDelayMsFor(text: String): Int {
        // Toy ile benzer hız: ~25..70 ms arası
        return (38 + text.length * 2).coerceIn(25, 70)
    }

    // Zamanlayıcı tabanlı kapanış kaldırıldı; yerine tur sayacı kullanılıyor
}


