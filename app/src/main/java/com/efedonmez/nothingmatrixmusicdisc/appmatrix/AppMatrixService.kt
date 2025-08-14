package com.efedonmez.nothingmatrixmusicdisc.appmatrix

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore

class AppMatrixService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = NowPlayingStore.getText()
        if (!text.isNullOrBlank()) {
            AppMatrixRenderer.renderText(applicationContext, text)
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}


