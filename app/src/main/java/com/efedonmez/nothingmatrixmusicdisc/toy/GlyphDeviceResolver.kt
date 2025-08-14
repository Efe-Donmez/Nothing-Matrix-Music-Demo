package com.efedonmez.nothingmatrixmusicdisc.toy

import android.content.Context
import android.os.Build
import com.nothing.ketchum.Glyph

data class GlyphDeviceConfig(
    val deviceCode: String,
    val gridWidth: Int,
    val gridHeight: Int
)

object GlyphDeviceResolver {
    private val fallbacks = listOf(
        Glyph.DEVICE_23111,
        Glyph.DEVICE_23113,
        Glyph.DEVICE_24111,
        Glyph.DEVICE_23112,
        Glyph.DEVICE_22111,
        Glyph.DEVICE_20111,
    )

    // Basit sezgisel eşleme (gerektikçe güncellenebilir)
    fun resolve(context: Context): GlyphDeviceConfig {
        val model = (Build.MODEL ?: "").lowercase()
        val product = (Build.PRODUCT ?: "").lowercase()
        val fingerprint = (Build.FINGERPRINT ?: "").lowercase()

        // Phone (3) ailesi: SDK dokümantasyonuna göre 25x25 int[] bekleniyor, deviceCode: 23112
        if (model.contains("phone") && (fingerprint.contains("phone_3") || product.contains("phone3"))) {
            return GlyphDeviceConfig(Glyph.DEVICE_23112, 25, 25)
        }

        // Varsayılan: 25x25
        return GlyphDeviceConfig(fallbacks.first(), 25, 25)
    }

    fun allFallbacks(): List<String> = fallbacks
}


