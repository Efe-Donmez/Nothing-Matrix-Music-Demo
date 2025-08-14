package com.efedonmez.nothingmatrixmusicdisc.settings

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    private const val PREFS = "nmmd_prefs"
    private const val KEY_APP_MODE_ENABLED = "app_mode_enabled"
    private const val KEY_MATRIX_RUNNING = "matrix_running"
    private const val KEY_DASHBOARD_SHOW_ART = "dashboard_show_art"
    private const val KEY_GLYPH_SHOW_ART = "glyph_show_art"
    private const val KEY_GLYPH_SHOW_TITLE = "glyph_show_title"
    private const val KEY_MATRIX_BRIGHTNESS = "matrix_brightness" // 0..255
    private const val KEY_MATRIX_CONTRAST = "matrix_contrast"     // 0..200, 100=normal
	private const val KEY_SERVICE_HEARTBEAT_TS = "service_heartbeat_ts"
    private const val KEY_SERVICE_DISABLED = "service_disabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAppModeEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_APP_MODE_ENABLED, false)

    fun setAppModeEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_APP_MODE_ENABLED, enabled).apply()
    }

    fun isMatrixRunning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MATRIX_RUNNING, false)

    fun setMatrixRunning(context: Context, running: Boolean) {
        prefs(context).edit().putBoolean(KEY_MATRIX_RUNNING, running).apply()
    }

    fun isDashboardShowArt(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DASHBOARD_SHOW_ART, true)

    fun setDashboardShowArt(context: Context, showArt: Boolean) {
        prefs(context).edit().putBoolean(KEY_DASHBOARD_SHOW_ART, showArt).apply()
    }
    
    fun isGlyphShowArt(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GLYPH_SHOW_ART, true)

    fun setGlyphShowArt(context: Context, showArt: Boolean) {
        prefs(context).edit().putBoolean(KEY_GLYPH_SHOW_ART, showArt).apply()
    }

    fun isGlyphShowTitle(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GLYPH_SHOW_TITLE, true)

    fun setGlyphShowTitle(context: Context, showTitle: Boolean) {
        prefs(context).edit().putBoolean(KEY_GLYPH_SHOW_TITLE, showTitle).apply()
    }

    fun getMatrixBrightness(context: Context): Int =
        prefs(context).getInt(KEY_MATRIX_BRIGHTNESS, 255).coerceIn(0, 255)

    fun setMatrixBrightness(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_MATRIX_BRIGHTNESS, value.coerceIn(0, 255)).apply()
    }

    fun getMatrixContrast(context: Context): Int =
        prefs(context).getInt(KEY_MATRIX_CONTRAST, 100).coerceIn(0, 200)

    fun setMatrixContrast(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_MATRIX_CONTRAST, value.coerceIn(0, 200)).apply()
    }

	fun setServiceHeartbeatNow(context: Context) {
		prefs(context).edit().putLong(KEY_SERVICE_HEARTBEAT_TS, System.currentTimeMillis()).apply()
	}

	fun getServiceHeartbeatTs(context: Context): Long =
		prefs(context).getLong(KEY_SERVICE_HEARTBEAT_TS, 0L)

    fun setServiceDisabled(context: Context, disabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_DISABLED, disabled).apply()
    }

    fun isServiceDisabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_DISABLED, false)
    
}


