package com.efedonmez.nothingmatrixmusicdisc.qs

import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.TileService

class QsTileBluetoothSettingsService : TileService() {
    override fun onClick() {
        super.onClick()
        val i = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(i)
    }
}


