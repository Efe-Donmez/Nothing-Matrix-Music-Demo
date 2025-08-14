package com.efedonmez.nothingmatrixmusicdisc.appmatrix

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphDeviceResolver
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphPreviewStore
import com.nothing.ketchum.GlyphMatrixManager

object AppMatrixControl {
    @Volatile private var manager: GlyphMatrixManager? = null
    @Volatile private var isInitialized = false
    @Volatile private var gridW = 25
    @Volatile private var gridH = 25
    private val handler = Handler(Looper.getMainLooper())
    private var pendingClose: Runnable? = null
    private var pendingClear: Runnable? = null

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
                // Firmware otomatik zaman aşımını aktif et (matrix kendisi kapatsın)
                try { mgr.setGlyphMatrixTimeout(true) } catch (_: Throwable) {}
                isInitialized = true
                after()
            }
            override fun onServiceDisconnected(name: ComponentName) { isInitialized = false }
        })
    }

    /** Dışarıya: manager'ı yeniden başlat (kayıt + grid boyutu) */
    fun reInit(context: Context) {
        ensureInit(context) { }
    }

    fun close(context: Context) {
        // Zamanlanmış close sırasında yeniden init bekleme; mevcut manager ile kapat
        val m = manager ?: return
        val zeros = IntArray(gridW * gridH) { 0 }
        try { m.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(gridW, gridH, zeros) } catch (_: Throwable) {}
        try { m.closeAppMatrix() } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(0, 0, intArrayOf()) } catch (_: Throwable) {}
        // manager'ı resetlemiyoruz; bir sonraki iş daha hızlı başlasın
    }

    fun clear(context: Context) {
        val m = manager ?: return
        val zeros = IntArray(gridW * gridH) { 0 }
        try { m.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(gridW, gridH, zeros) } catch (_: Throwable) {}
    }

    /** Zamanlayıcıda manager null riskini önlemek için, render anındaki manager ile temizle */
    fun clearWith(m: GlyphMatrixManager, width: Int, height: Int) {
        val zeros = IntArray(width * height) { 0 }
        try { m.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(width, height, zeros) } catch (_: Throwable) {}
    }

    fun closeAfter(context: Context, delayMs: Long) {
        // 🛑 Önceki kapanma işlemlerini iptal et
        pendingClear?.let { handler.removeCallbacks(it) }
        pendingClose?.let { handler.removeCallbacks(it) }
        if (delayMs <= 100) {
            clear(context)
            val r = Runnable { close(context) }
            pendingClose = r
            handler.postDelayed(r, 100)
            return
        }
        val clearR = Runnable { clear(context) }
        pendingClear = clearR
        handler.postDelayed(clearR, delayMs)
        val closeR = Runnable { close(context) }
        pendingClose = closeR
        handler.postDelayed(closeR, delayMs + 250)
    }

    /** Zamanlayıcıda manager null riskini önlemek için, render anındaki manager ile kapat */
    fun closeWith(m: GlyphMatrixManager, width: Int, height: Int) {
        val zeros = IntArray(width * height) { 0 }
        try { m.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(width, height, zeros) } catch (_: Throwable) {}
        try { m.closeAppMatrix() } catch (_: Throwable) {}
        try { GlyphPreviewStore.update(0, 0, intArrayOf()) } catch (_: Throwable) {}
    }

    /** Dışarıdan zamanlanmış clear/close işlemlerini iptal eder. */
    fun cancelScheduled() {
        pendingClear?.let { handler.removeCallbacks(it) }
        pendingClose?.let { handler.removeCallbacks(it) }
        pendingClear = null
        pendingClose = null
    }

    /**
     * Force reset: Stuck durumlar için birkaç defa siyah frame basıp kapatır,
     * ardından manager'ı sıfırlar (bir dahaki kullanımda yeniden init olur).
     */
    // forceReset kaldırıldı
}


