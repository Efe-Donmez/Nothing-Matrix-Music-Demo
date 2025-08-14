package com.efedonmez.nothingmatrixmusicdisc.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NotificationAccess

/**
 * 🔐 İzin Yardımcısı
 * 
 * Uygulama için gerekli izinleri kontrol eder ve ister:
 * ✅ Bildirim erişimi
 * ✅ Pil optimizasyonu muafiyeti
 * ✅ Kullanıcı dostu bilgilendirme
 */
object PermissionHelper {
    
    /**
     * 🚀 Tüm izinleri kontrol et ve iste
     */
    fun checkAllPermissions(context: Context): Boolean {
        var allGranted = true
        
        // 1. Bildirim erişimi kontrol et
        if (!NotificationAccess.checkAndRequestPermission(context)) {
            allGranted = false
        }
        
        // 2. Pil optimizasyonu kontrol et
        if (!isBatteryOptimizationDisabled(context)) {
            requestBatteryOptimizationDisable(context)
            allGranted = false
        }
        
        return allGranted
    }
    
    /**
     * 🔋 Pil optimizasyonu devre dışı mı kontrol et
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true // Eski versiyonlarda bu ayar yok
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * ⚡ Pil optimizasyonu muafiyeti iste
     */
    fun requestBatteryOptimizationDisable(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                UiNotifier.show(context, "Arka plan çalışması için pil optimizasyonunu kapatın")
                
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            // Hata durumunda genel pil ayarlarını aç
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {
                UiNotifier.show(context, "Pil ayarları açılamadı")
            }
        }
    }
    
    /**
     * 📋 İzin durumu raporu
     */
    fun getPermissionStatus(context: Context): String {
        val notificationAccess = NotificationAccess.isEnabled(context)
        val batteryOptimization = isBatteryOptimizationDisabled(context)
        
        return buildString {
            appendLine("📱 İzin Durumu:")
            appendLine("🔔 Bildirim Erişimi: ${if (notificationAccess) "✅ Açık" else "❌ Kapalı"}")
            appendLine("🔋 Pil Optimizasyonu: ${if (batteryOptimization) "✅ Devre Dışı" else "❌ Aktif"}")
            
            if (!notificationAccess || !batteryOptimization) {
                appendLine()
                appendLine("⚠️ Eksik izinler uygulamanın düzgün çalışmasını engelleyebilir")
            }
        }
    }
}
