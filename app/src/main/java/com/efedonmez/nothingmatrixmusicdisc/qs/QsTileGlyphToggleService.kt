package com.efedonmez.nothingmatrixmusicdisc.qs

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.content.Intent
import com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphToyService

class QsTileGlyphToggleService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val running = isServiceHealthy()
        if (running) {
            startService(Intent(this, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_STOP))
            AppSettings.setMatrixRunning(this, false)
            AppSettings.setServiceDisabled(this, true)
        } else {
            AppSettings.setServiceDisabled(this, false)
            startService(Intent(this, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_START))
            AppSettings.setMatrixRunning(this, true)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = isServiceHealthy()
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (running) "Glyph: Açık" else "Glyph: Kapalı"
        tile.updateTile()
    }

    // Servis sağlığı: isMatrixRunning + heartbeat son 15 sn içinde güncellenmiş olmalı
    private fun isServiceHealthy(): Boolean {
        val running = AppSettings.isMatrixRunning(this)
        val ts = AppSettings.getServiceHeartbeatTs(this)
        val alive = System.currentTimeMillis() - ts < 15_000
        return running && alive
    }
}


