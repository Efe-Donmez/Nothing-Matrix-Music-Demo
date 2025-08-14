package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.efedonmez.nothingmatrixmusicdisc.util.UiNotifier

/**
 * 🔔 Bildirim Erişim Kontrolü
 * 
 * Bu sınıf bildirim okuma yetkisini kontrol eder:
 * ✅ İzin durumunu kontrol et
 * ✅ İzin yoksa otomatik iste
 * ✅ Ayarlar sayfasını aç
 * ✅ Kullanıcıya rehberlik et
 */
object NotificationAccess {

    /**
     * 🔍 Bildirim erişim yetkisi var mı kontrol et
     */
    fun isEnabled(context: Context): Boolean {
        return try {
            val pkgName = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            
            if (TextUtils.isEmpty(flat)) return false
            
            val cn = ComponentName(pkgName, NowPlayingListenerService::class.java.name)
            flat.split(":").any {
                try {
                    ComponentName.unflattenFromString(it)?.equals(cn) == true
                } catch (_: Throwable) {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 🚀 İzin kontrolü yap ve gerekirse iste
     */
    fun checkAndRequestPermission(context: Context): Boolean {
        if (isEnabled(context)) {
            // İzin var - rebind yap
            requestRebindIfPossible(context)
            return true
        } else {
            // İzin yok - kullanıcıya bildir ve ayarlara yönlendir
            UiNotifier.show(context, "Müzik bilgilerini okumak için bildirim erişimi gerekli")
            openSettings(context)
            return false
        }
    }

    /**
     * ⚙️ Bildirim ayarlarını aç
     */
    fun openSettings(context: Context) {
        try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            UiNotifier.show(context, "Ayarlar açılamadı")
        }
    }

    /**
     * 🔄 Servisi yeniden bağla (izin varsa)
     */
    fun requestRebindIfPossible(context: Context) {
        if (!isEnabled(context)) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, NowPlayingListenerService::class.java)
                )
            }
        } catch (e: Exception) {
            // Sessiz başarısızlık
        }
    }
}


