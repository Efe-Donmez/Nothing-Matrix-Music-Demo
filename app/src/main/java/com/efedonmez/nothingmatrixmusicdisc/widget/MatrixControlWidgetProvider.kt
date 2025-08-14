package com.efedonmez.nothingmatrixmusicdisc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.RemoteViews
import com.efedonmez.nothingmatrixmusicdisc.R
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphToyService

class MatrixControlWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_matrix_control)

            views.setOnClickPendingIntent(R.id.btn_start, piService(context, GlyphToyService.ACTION_START))
            views.setOnClickPendingIntent(R.id.btn_stop, piService(context, GlyphToyService.ACTION_STOP))
            views.setOnClickPendingIntent(R.id.btn_line, piService(context, GlyphToyService.ACTION_SHOW_LINE))
            views.setOnClickPendingIntent(R.id.btn_rect, piService(context, GlyphToyService.ACTION_SHOW_RECT))
            views.setOnClickPendingIntent(R.id.btn_circle, piService(context, GlyphToyService.ACTION_SHOW_CIRCLE))

            // Hızlı ayarlar: Wi‑Fi ve Bluetooth paneli
            views.setOnClickPendingIntent(R.id.btn_wifi, piSettingsPanel(context, Settings.Panel.ACTION_WIFI))
            // ACTION_BLUETOOTH bazı sürümlerde mevcut değildir; ayar ekranına yönlendirelim
            views.setOnClickPendingIntent(R.id.btn_bt, piSettingsPanel(context, Settings.ACTION_BLUETOOTH_SETTINGS))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun piService(context: Context, action: String): PendingIntent {
        val intent = Intent(context, GlyphToyService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun piSettingsPanel(context: Context, panelAction: String): PendingIntent {
        val intent = Intent(panelAction)
        return PendingIntent.getActivity(
            context,
            panelAction.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}


