package com.efedonmez.nothingmatrixmusicdisc.matrix

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings
import com.efedonmez.nothingmatrixmusicdisc.util.TextSanitizer
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphDeviceResolver
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphPreviewStore
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils

/**
 * 🎯 Basit ama doğru Matrix kontrolü (resim ve yazı)
 * - Cihazı kaydeder, doğru grid boyutunu kullanır
 * - Bitmap → int[] dönüştürüp hem önizleme hem Matrix'e gönderir
 * - 10 sn sonra otomatik kapatır (resim), 5 sn (yazı)
 */
object MatrixController {

    private var manager: GlyphMatrixManager? = null
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var isInitialized: Boolean = false
    @Volatile private var gridW: Int = 25
    @Volatile private var gridH: Int = 25
    private var closeTimer: Runnable? = null
    private var marqueeTimer: Runnable? = null
    private var isMarqueeRunning: Boolean = false
    private var lastText: String = ""
    private var lastWidth: Int = 0
    private var scrollX: Int = 0
    private var loopCount: Int = 0

    private fun ensureInit(context: Context, after: () -> Unit) {
        val m = manager
        if (isInitialized && m != null) { after(); return }
        val mgr = GlyphMatrixManager.getInstance(context.applicationContext)
        manager = mgr
        mgr.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName) {
                val cfg = GlyphDeviceResolver.resolve(context)
                try { mgr.register(cfg.deviceCode) } catch (_: Throwable) {}
                gridW = cfg.gridWidth
                gridH = cfg.gridHeight
                isInitialized = true
                after()
            }
            override fun onServiceDisconnected(name: ComponentName) { isInitialized = false }
        })
    }

    /** Resmi Matrix'e gönder ve 10 sn sonra kapat. */
    fun showImage(context: Context, bitmap: Bitmap?) {
        if (bitmap == null) return
        // Önceki kapanma zamanlayıcısını iptal et
        closeTimer?.let { handler.removeCallbacks(it) }
        ensureInit(context) {
            try {
                val b = AppSettings.getMatrixBrightness(context)
                val c = AppSettings.getMatrixContrast(context)
                val pixels = try {
                    // Not: SDK'da scale=0 (auto), threshold=128, extra=255 önerildi
                    GlyphMatrixUtils.convertToGlyphMatrix(
                        bitmap, gridW, gridH,
                        0, 128, 255,
                        false, false
                    )
                } catch (_: Throwable) { IntArray(gridW * gridH) { 0 } }

                // Önizlemeyi güncelle
                GlyphPreviewStore.update(gridW, gridH, pixels)
                // Matrix'e gönder
                manager?.setAppMatrixFrame(pixels)
                // 10 sn sonra otomatik kapat
                closeTimer = Runnable { close(context) }
                handler.postDelayed(closeTimer!!, 10_000)
            } catch (_: Throwable) {
                // Sessizce geç, kapanma dener
                close(context)
            }
        }
    }

    /** Kayan yazı: 2 tam turdan sonra kapatır. */
    fun showText(context: Context, text: String?) {
        val raw = text?.trim() ?: return
        val sanitized = TextSanitizer.sanitizeForGlyph(raw)
        if (sanitized.isBlank()) return

        // Önceki zamanlayıcı/animasyonları iptal et
        closeTimer?.let { handler.removeCallbacks(it) }
        marqueeTimer?.let { handler.removeCallbacks(it) }
        isMarqueeRunning = false

        ensureInit(context) {
            // Başlangıç durumunu hazırla
            lastText = sanitized
            lastWidth = estimateTextWidthCells(lastText)
            scrollX = gridW + 12 // giriş boşluğu
            loopCount = 0
            isMarqueeRunning = true

            // Animasyonu başlat
            marqueeTimer = object : Runnable {
                override fun run() {
                    if (!isMarqueeRunning) return
                    val m = manager ?: return
                    try {
                        val obj = GlyphMatrixObject.Builder()
                            .setText(lastText)
                            .setPosition(scrollX, (gridH / 2).coerceAtLeast(1))
                            .setScale(100)
                            .setBrightness(255)
                            .build()

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(obj)
                            .build(context)

                        m.setAppMatrixFrame(frame)

                        // Kaydır
                        scrollX -= 1
                        val endX = -lastWidth
                        if (scrollX < endX) {
                            loopCount += 1
                            if (loopCount >= 2) {
                                // Kapat
                                isMarqueeRunning = false
                                closeTimer = Runnable { close(context) }
                                handler.postDelayed(closeTimer!!, 100)
                                return
                            } else {
                                scrollX = gridW // yeni tura başla
                            }
                        }

                        val delay = normalDelayMsFor(lastText).toLong()
                        marqueeTimer = this
                        handler.postDelayed(this, delay)
                    } catch (_: Throwable) {
                        isMarqueeRunning = false
                        close(context)
                    }
                }
            }
            handler.post(marqueeTimer!!)
        }
    }

    private fun estimateTextWidthCells(text: String): Int {
        return (text.length * 6).coerceAtLeast(8).coerceAtMost(200)
    }

    private fun normalDelayMsFor(text: String): Int {
        return (38 + text.length * 2).coerceIn(25, 70)
    }

    /** Matrix'i siyaha çevirip kapatır ve önizlemeyi temizler. */
    fun close(context: Context) {
        try {
            closeTimer?.let { handler.removeCallbacks(it) }
            val w = gridW
            val h = gridH
            val zeros = IntArray(w * h) { 0 }
            try { manager?.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
            try { manager?.closeAppMatrix() } catch (_: Throwable) {}
        } finally {
            GlyphPreviewStore.update(0, 0, intArrayOf())
        }
    }
}
