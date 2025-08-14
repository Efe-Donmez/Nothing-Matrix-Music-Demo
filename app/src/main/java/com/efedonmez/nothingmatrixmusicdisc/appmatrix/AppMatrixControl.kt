package com.efedonmez.nothingmatrixmusicdisc.appmatrix

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphDeviceResolver
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
                isInitialized = true
                after()
            }
            override fun onServiceDisconnected(name: ComponentName) { isInitialized = false }
        })
    }

    fun close(context: Context) {
        ensureInit(context) {
            // Clear first (bazı sürümlerde close no-op olabilir)
            val zeros = IntArray(gridW * gridH) { 0 }
            try { manager?.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
            try { manager?.closeAppMatrix() } catch (_: Throwable) {}
        }
    }

    fun clear(context: Context) {
        ensureInit(context) {
            val zeros = IntArray(gridW * gridH) { 0 }
            try { manager?.setAppMatrixFrame(zeros) } catch (_: Throwable) {}
        }
    }

    fun closeAfter(context: Context, delayMs: Long) {
        // 🛑 Önceki kapanma işlemlerini iptal et
        pendingClear?.let { handler.removeCallbacks(it) }
        pendingClose?.let { handler.removeCallbacks(it) }
        
        if (delayMs <= 100) {
            // ⚡ Hemen kapat (100ms veya daha az)
            clear(context)
            handler.postDelayed({ close(context) }, 100)
        } else {
            // ⏰ Zamanlı kapanma
            // Step 1: Siyah ekran göster
            val clearR = Runnable { 
                clear(context)
                // Step 2: 500ms sonra tamamen kapat
                handler.postDelayed({ close(context) }, 500)
            }
            pendingClear = clearR
            handler.postDelayed(clearR, delayMs)
        }
        
        // Referansları temizle
        pendingClose = null
    }
}


