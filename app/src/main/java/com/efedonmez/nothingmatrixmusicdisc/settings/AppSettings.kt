package com.efedonmez.nothingmatrixmusicdisc.settings

import android.content.Context
import android.content.SharedPreferences

object AppSettings {
    private const val PREFS = "nmmd_prefs"
    private const val KEY_APP_MODE_ENABLED = "app_mode_enabled"
    private const val KEY_MATRIX_RUNNING = "matrix_running"

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
    
}


