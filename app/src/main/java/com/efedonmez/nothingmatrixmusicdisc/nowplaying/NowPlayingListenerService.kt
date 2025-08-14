package com.efedonmez.nothingmatrixmusicdisc.nowplaying

import android.app.Notification
import android.graphics.Bitmap
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * 🎵 Müzik Dinleyici Servisi
 * 
 * Bu servis şunları yapar:
 * 📱 Telefonun bildirimlerini dinler
 * 🎵 Müzik uygulamalarındaki değişiklikleri yakalar  
 * 🖼️ Albüm kapağı ve şarkı bilgisini alır
 * 📺 Matrix'te güncellenen müziği gösterir
 * ⏰ 10 saniye sonra Matrix'i kapatır
 */
class NowPlayingListenerService : NotificationListenerService() {

    companion object {
        // 🕐 Son Matrix güncelleme zamanı (çok sık güncellemeyi engeller)
        @Volatile private var lastRenderMs: Long = 0L
        // ⏱️ Yeni güncelleme için minimum bekleme süresi (10 saniye)
        private const val RENDER_WINDOW_MS: Long = 10_000
        // 📝 Son müzik bilgisi (değişiklik kontrolü için)
        @Volatile private var lastMusicInfo: String? = null
    }

	/**
	 * 📱 Yeni bildirim geldiğinde çalışır
	 * Özellikle müzik uygulamalarının bildirimlerini yakalayıp işler
	 */
	override fun onNotificationPosted(sbn: StatusBarNotification?) {
		sbn ?: return                    // Bildirim yoksa çık
		val n = sbn.notification ?: return // Notification objesi yoksa çık
		
		// 📊 Bildirimden müzik verilerini çıkar
        val extras = n.extras
		val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()       // Şarkı adı
		val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()         // Genelde sanatçı
		val sub = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()      // Alt yazı (sanatçı/albüm)
		val info = extras?.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()    // Ekstra bilgi
        val album = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()    // Albüm adı
        
        // 🖼️ Albüm kapağını al (büyük ikon olarak)
        val largeIcon = (extras?.get(Notification.EXTRA_LARGE_ICON) as? Bitmap)
            ?: (n.getLargeIcon()?.loadDrawable(this)?.let { d ->
                try { com.nothing.ketchum.GlyphMatrixUtils.drawableToBitmap(d) } catch (_: Throwable) { null }
            })

		// 🎯 Sanatçı adını bul (çeşitli alanlardan)
		val artistCandidates = listOfNotNull(sub, info)
		val artist = artistCandidates.firstOrNull { !it.isNullOrBlank() } ?: run {
			// Bazen EXTRA_TEXT içinde "Artist — Title" gibi olabilir
			val t = text?.trim().orEmpty()
			if (t.contains(" - ")) t.substringBefore(" - ") else null
		}
		
		// 🎵 Şarkı adını bul (title boşsa text içinde ara)
		val inferredTitle = title ?: run {
			val t = text?.trim().orEmpty()
			if (t.contains(" - ")) t.substringAfter(" - ") else t.ifEmpty { null }
		}

		// 📝 Müzik bilgisini güncelle
        if (!inferredTitle.isNullOrBlank() || !artist.isNullOrBlank()) {
            NowPlayingStore.update(inferredTitle, artist, album = album, art = largeIcon, isPlaying = true, posMs = null, durMs = null, pkg = sbn.packageName)
		} else {
			// Son çare: tek satır halinde kaydet
			val line = listOfNotNull(title, text, sub, info).firstOrNull { it.isNotBlank() }
            NowPlayingStore.updateFromSingleLine(line, pkg = sbn.packageName)
		}
		
		// 🔄 Müzik değişip değişmediğini kontrol et
		val currentMusicInfo = "${inferredTitle}_${artist}_${largeIcon?.hashCode()}"
		val musicChanged = currentMusicInfo != lastMusicInfo
		lastMusicInfo = currentMusicInfo

        // 🎯 Sadece müzik değiştiyse ve otomatik gösterim açıksa Matrix'i güncelle
        if (!musicChanged) return  // Müzik değişmediyse hiçbir şey yapma
        
        // ⏰ 10 saniye içinde tekrar güncellemeyi engelle (kapanmayı bozmasın)
        val nowTs = System.currentTimeMillis()
        if (nowTs - lastRenderMs < RENDER_WINDOW_MS) return
        
        // 🔧 Sadece servis çalışıyorsa otomatik gösterim yap
        val ctx = applicationContext
        if (!com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.isMatrixRunning(ctx)) return
        
        try {
            // 🎨 Kullanıcı tercihine göre Matrix'i güncelle (görsel ya da metin)
            val showArt = com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.isGlyphShowArt(ctx)
            if (showArt) {
                // Görsel mod: albüm kapağını göster
                com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(ctx)
            } else {
                // Metin mod: şarkı bilgisini göster
                NowPlayingStore.getText()?.takeIf { it.isNotBlank() }?.let { 
                    com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixRenderer.renderText(ctx, it) 
                }
            }
            
            // ⏰ closeAfter çağrısı AppMatrixImageRenderer/AppMatrixRenderer içinde yapılıyor
            lastRenderMs = nowTs
        } catch (_: Throwable) {
            // Hata durumunda sessizce devam et
        }
	}
}


