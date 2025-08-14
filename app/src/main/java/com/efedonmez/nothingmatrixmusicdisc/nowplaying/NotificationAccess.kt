package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        if (TextUtils.isEmpty(flat)) return false
        val cn = ComponentName(pkgName, NowPlayingListenerService::class.java.name)
        return flat.split(":").any {
            try {
                ComponentName.unflattenFromString(it)?.equals(cn) == true
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun openSettings(context: Context) {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun requestRebindIfPossible(context: Context) {
        if (!isEnabled(context)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, NowPlayingListenerService::class.java)
                )
            }
        } catch (_: Throwable) {
        }
    }
}


