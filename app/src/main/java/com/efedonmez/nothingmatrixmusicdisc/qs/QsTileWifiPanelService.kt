package com.efedonmez.nothingmatrixmusicdisc.qs

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.TileService

class QsTileWifiPanelService : TileService() {
    override fun onClick() {
        super.onClick()
        val i = Intent(Settings.Panel.ACTION_WIFI)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(i)
    }
}


