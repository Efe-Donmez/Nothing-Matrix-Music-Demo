package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.app.Notification
import android.graphics.Bitmap
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NowPlayingListenerService : NotificationListenerService() {

	override fun onNotificationPosted(sbn: StatusBarNotification?) {
		sbn ?: return
		val n = sbn.notification ?: return
		// Medya stilindeki bildirimlerden başlık/alt başlığı çek
        val extras = n.extras
		val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
		val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
		val sub = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
		val info = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
        val album = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val largeIcon = (extras?.get(Notification.EXTRA_LARGE_ICON) as? Bitmap)
            ?: (n.getLargeIcon()?.loadDrawable(this)?.let { d ->
                try { com.nothing.ketchum.GlyphMatrixUtils.drawableToBitmap(d) } catch (_: Throwable) { null }
            })

		// Yaygın müzik uygulamaları genelde başlık=şarkı, text=subText=artist vs.
		val artistCandidates = listOfNotNull(sub, info)
		val artist = artistCandidates.firstOrNull { !it.isNullOrBlank() } ?: run {
			// Bazen EXTRA_TEXT içinde "Artist — Title" gibi olabilir
			val t = text?.trim().orEmpty()
			if (t.contains(" - ")) t.substringBefore(" - ") else null
		}
		// Title boşsa text içinde aramayı dene
		val inferredTitle = title ?: run {
			val t = text?.trim().orEmpty()
			if (t.contains(" - ")) t.substringAfter(" - ") else t.ifEmpty { null }
		}

        if (!inferredTitle.isNullOrBlank() || !artist.isNullOrBlank()) {
            NowPlayingStore.update(inferredTitle, artist, album = album, art = largeIcon, isPlaying = true, posMs = null, durMs = null, pkg = sbn.packageName)
		} else {
			// Son çare tek satır
			val line = listOfNotNull(title, text, sub, info).firstOrNull { it.isNotBlank() }
            NowPlayingStore.updateFromSingleLine(line, pkg = sbn.packageName)
		}

        // App-mode Matrix: ayara bağlı olarak güncelle
        try {
            if (com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.isAppModeEnabled(applicationContext)) {
                val now = NowPlayingStore.getText()
                if (!now.isNullOrBlank()) {
                    com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixRenderer.renderText(applicationContext, now)
                }
            }
        } catch (_: Throwable) {}
	}
}


