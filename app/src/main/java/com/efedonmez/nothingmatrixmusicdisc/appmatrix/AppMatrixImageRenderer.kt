package com.efedonmez.nothingmatrixmusicdisc.appmatrix

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphDeviceResolver
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphPreviewStore
import com.nothing.ketchum.GlyphMatrixManager

object AppMatrixImageRenderer {

    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var manager: GlyphMatrixManager? = null
    @Volatile private var isInitialized: Boolean = false
    @Volatile private var gridW: Int = 25
    @Volatile private var gridH: Int = 25

    fun renderNowPlayingArt(context: Context) {
        val art = NowPlayingStore.getInfo()?.art ?: return
        renderBitmap(context, art)
    }

    fun renderBitmap(context: Context, bitmap: Bitmap) {
        // 🛑 Önceki işlemleri iptal et
        handler.removeCallbacksAndMessages(null)
        
        ensureInit(context) {
            val w = gridW
            val h = gridH
            val brightness = com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.getMatrixBrightness(context)
            val contrast = com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.getMatrixContrast(context)
            val pixels = convertBitmapToGlyph(bitmap, w, h, brightness, contrast)
            
            // 📸 Önizlemeyi güncelle
            GlyphPreviewStore.update(w, h, pixels)
            
            try {
                // 📺 Matrix'te göster
                manager?.setAppMatrixFrame(pixels)
                
                // ⏰ 10 saniye sonra otomatik kapat
                AppMatrixControl.closeAfter(context, 10_000)
            } catch (e: Throwable) {
                com.efedonmez.nothingmatrixmusicdisc.util.UiNotifier.show(context, "Matrix render hatası: ${e.message}")
            }
        }
    }

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
            override fun onServiceDisconnected(name: ComponentName) {
                isInitialized = false
            }
        })
    }

    // Basit, güvenli dönüştürücü: bitmap'i grid'e ölçekler ve gri ton parlaklığı 0..255 üretir
    private fun convertBitmapToGlyph(src: Bitmap, targetW: Int, targetH: Int, targetBrightness: Int = 255, targetContrast: Int = 100): IntArray {
        val bmp = Bitmap.createScaledBitmap(src, targetW, targetH, true)
        val out = IntArray(targetW * targetH)
        var i = 0
        for (y in 0 until targetH) {
            for (x in 0 until targetW) {
                val c = bmp.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                var lum = (0.2126f * r + 0.7152f * g + 0.0722f * b)
                // Kontrast (merkez 128 etrafında)
                val k = (targetContrast / 100f)
                lum = ((lum - 128f) * k + 128f)
                // Parlaklık ölçeği
                lum = lum * (targetBrightness / 255f)
                out[i++] = lum.toInt().coerceIn(0, 255)
            }
        }
        return out
    }
}


