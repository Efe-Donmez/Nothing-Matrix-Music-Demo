package com.efedonmez.nothingmatrixmusicdisc.toy

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore
import com.efedonmez.nothingmatrixmusicdisc.matrix.ShapeRenderer
import com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings
import com.efedonmez.nothingmatrixmusicdisc.util.TextSanitizer
import com.nothing.ketchum.GlyphMatrixUtils
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlin.math.roundToInt
import org.json.JSONArray
import com.efedonmez.nothingmatrixmusicdisc.util.UiNotifier

/**
 * ✨ GlyphToyService - Nothing Phone Glyph Matrix Kontrolü ✨
 *
 * Bu servis Nothing Phone'un arkasındaki Matrix ekranını kontrol eder.
 * Basit olarak şunları yapar:
 * 
 * 📱 Matrix'te yazı gösterir (kayan yazı)
 * 🎵 Matrix'te müzik albüm kapağı gösterir
 * 🔧 Matrix'i açar/kapatır
 * 🎨 Basit şekiller çizer (çizgi, kare, daire)
 */
class GlyphToyService : Service() {

	companion object {
		// ⚡ Servis komutları - Bu komutlarla Matrix'e ne yapacağını söylüyoruz
		const val ACTION_START = "com.efedonmez.nothingmatrixmusicdisc.ACTION_START"        // Servisi başlat
		const val ACTION_STOP = "com.efedonmez.nothingmatrixmusicdisc.ACTION_STOP"          // Servisi durdur
		const val ACTION_SHOW_LINE = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_LINE" // Çizgi çiz
		const val ACTION_SHOW_RECT = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_RECT" // Kare çiz
		const val ACTION_SHOW_CIRCLE = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_CIRCLE" // Daire çiz
		const val ACTION_SHOW_DISC = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_DISC"     // Albüm kapağı göster
		const val ACTION_SHOW_SAMPLE_IMAGE = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_SAMPLE_IMAGE" // Örnek resim
		const val ACTION_SHOW_PIXEL_DATA = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_PIXEL_DATA"     // Ham pixel verisi
		const val EXTRA_PIXEL_DATA = "com.efedonmez.nothingmatrixmusicdisc.EXTRA_PIXEL_DATA" // Pixel verisi parametresi

		// 📏 Matrix boyutları - Telefona göre otomatik ayarlanır
        @Volatile var currentGridWidth = 40   // Matrix genişliği (pixel sayısı)
        @Volatile var currentGridHeight = 25  // Matrix yüksekliği (pixel sayısı)

		/**
		 * 🚀 Kolay servis başlatma fonksiyonu
		 * Başka yerlerden servisi başlatmak için kullanılır
		 */
		fun startService(context: Context, action: String) {
			val intent = Intent(context, GlyphToyService::class.java).setAction(action)
			context.startService(intent)
		}
	}

	// 🧠 Matrix kontrol sistemi
	private var glyphManager: GlyphMatrixManager? = null  // Nothing'in Matrix API'si
	private val handler = Handler(Looper.getMainLooper()) // Zamanlama için
	
	// 🎯 Durum kontrolleri - Servisin ne yaptığını takip eder
	private var isRunning = false        // Servis çalışıyor mu?
    private var isDiscAnimating = false  // Albüm kapağı dönüyor mu?
    private var isToyBound = false       // Sistem Toy olarak seçmiş mi?
	
	// 📜 Kayan yazı ayarları
	private var scrollX = 40                           // Yazının X konumu (soldan sağa)
	private var marqueeText: String = "Efe Donmez"     // Varsayılan yazı
	private var lastText: String = marqueeText         // Önceki yazı (değişiklik kontrolü için)
	private var lastWidth: Int = 40                    // Yazının genişliği (pixel cinsinden)
	private val resetGap = 0                           // Yazı çıktıktan sonraki boşluk
	private val entryPadding = 12                      // Yazının başlangıç boşluğu
	private val loopPadding = 0                        // Döngü arası boşluk
    private var loopCount: Int = 0                     // Kaç kez tam tur yaptı (2 turdan sonra duracak)
    private var discAngleDeg: Float = 0f               // Plak döndürme açısı (derece)

	/**
	 * 📏 Yazı genişliğini hesaplar
	 * Her harf yaklaşık 6 pixel genişliğinde
	 * Minimum 8, maksimum 120 pixel olur
	 */
	private fun estimateTextWidthCells(text: String): Int {
		return (text.length * 6).coerceAtLeast(8).coerceAtMost(120)
	}

	/**
	 * ⏱️ Yazı kaydırma hızını hesaplar
	 * Kısa yazılar hızlı (25ms), uzun yazılar yavaş (70ms) kayar
	 */
	private fun normalDelayMsFor(text: String): Int {
		return (38 + text.length * 2).coerceIn(25, 70)
	}

    /**
     * 🔗 Sistem servisimizi "Glyph Toy" olarak bağladığında çalışır
     * Bu fonksiyon sistem ayarlarından Toy seçildiğinde tetiklenir
     */
    override fun onBind(intent: Intent?): IBinder? {
        isToyBound = true  // Sistem bizi Toy olarak seçti
        init()             // Matrix bağlantısını başlat
        return null
    }

	/**
	 * 🔌 Sistem servisimizi Toy'dan çıkardığında çalışır
	 * Tüm işlemleri durdurur ve Matrix'i kapatır
	 */
	override fun onUnbind(intent: Intent?): Boolean {
		isRunning = false                           // Çalışmayı durdur
		handler.removeCallbacksAndMessages(null)    // Bekleyen işleri iptal et
		glyphManager?.turnOff()                     // Matrix'i kapat
		glyphManager?.unInit()                      // Matrix bağlantısını kes
		glyphManager = null                         // Bellekten temizle
		AppSettings.setMatrixRunning(this, false)   // Ayarlarda "kapalı" yap
        isToyBound = false                          // Artık Toy değiliz
		return false
	}

	/**
	 * 🎬 Ana komut merkezi - Uygulamadan gelen komutları işler
	 * Bu fonksiyon her Intent geldiğinde çalışır (START, STOP, SHOW_DISC vs.)
	 */
	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 💓 Hayat belirtisi - Quick Settings için (servis çalışıyor mu?)
        AppSettings.setServiceHeartbeatNow(this)
        
        // 🚫 Eğer servis devre dışı bırakılmışsa hiçbir şey yapma (STOP hariç)
        if (AppSettings.isServiceDisabled(this) && intent?.action != ACTION_STOP) {
            stopSelf(startId)  // Servisi kapat
            return START_NOT_STICKY
        }
        
        val action = intent?.action  // Ne yapacağımızı öğren
        
        // 🎯 Eğer sistem bizi Toy olarak seçmemişse, App Matrix kullan
        if (!isToyBound && action != ACTION_STOP) {
            // Toy seçili değilken: App Matrix kanalını kullan (Toy zorunlu değil)
            when (action) {
                ACTION_START -> {
                    if (com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.isGlyphShowArt(this)) {
                        // Albüm kapağını App Matrix'te göster
                        try { com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(applicationContext) } catch (_: Throwable) {}
                    } else {
                        // Yazıyı App Matrix'te kaydır
                        val text = com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore.getText() ?: lastText
                        try { com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixRenderer.renderText(applicationContext, text) } catch (_: Throwable) {}
                    }
                }
                ACTION_SHOW_DISC -> {
                    try { com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(applicationContext) } catch (_: Throwable) {}
                }
                ACTION_SHOW_SAMPLE_IMAGE -> {
                    // Örnek görseli App Matrix'te göster (uygulama ikonu)
                    val ctx = applicationContext
                    val d = try { androidx.core.content.ContextCompat.getDrawable(ctx, com.efedonmez.nothingmatrixmusicdisc.R.mipmap.ic_launcher) } catch (_: Throwable) { null }
                    val bmp = d?.let { try { com.nothing.ketchum.GlyphMatrixUtils.drawableToBitmap(it) } catch (_: Throwable) { null } }
                    if (bmp != null) {
                        try { com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderBitmap(applicationContext, bmp) } catch (_: Throwable) {}
                    }
                }
                ACTION_SHOW_PIXEL_DATA -> {
                    // Pixel data için App Matrix desteği basitçe doğrudan setAppMatrixFrame ile yapılabilir;
                    // Ancak burada basitleştiriyoruz: önizleme zaten var, AppMatrix yolu ayrı butonla çağrılabilir.
                    renderPixelDataPreviewOnly(intent)
                }
                else -> {}
            }
            return START_NOT_STICKY
        }
        when (action) {
            ACTION_START -> ensureInit {
				AppSettings.setMatrixRunning(this, true)
				// Kullanıcı tercihi: Glyph'te görsel mi yazı mı?
				if (com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings.isGlyphShowArt(this)) {
                    // Önce basit ham albüm resmi gösterimi (döndürmesiz) — Matrix Toy'a direkt yaz
                    renderAlbumArtRawToy()
                } else {
					startMarquee()
				}
			}
			ACTION_STOP -> {
				isRunning = false
				isDiscAnimating = false
				handler.removeCallbacksAndMessages(null)
            try { glyphManager?.turnOff() } catch (e: Throwable) { UiNotifier.show(this, "turnOff hata: ${e.message}") }
            try { glyphManager?.unInit() } catch (e: Throwable) { UiNotifier.show(this, "unInit hata: ${e.message}") }
				glyphManager = null
				AppSettings.setMatrixRunning(this, false)
                AppSettings.setServiceDisabled(this, true)
				stopSelf(startId)
			}
			ACTION_SHOW_LINE -> ensureInit { renderShapeLine() }
			ACTION_SHOW_RECT -> ensureInit { renderShapeRect() }
			ACTION_SHOW_CIRCLE -> ensureInit { renderShapeCircle() }
			ACTION_SHOW_DISC -> ensureInit { startDisc() }
			ACTION_SHOW_SAMPLE_IMAGE -> ensureInit { renderSampleImage() }
			ACTION_SHOW_PIXEL_DATA -> ensureInit { renderPixelDataFromIntent(intent) }
			else -> ensureInit { startMarquee() }
		}
		return START_NOT_STICKY
	}

	/**
	 * Glyph servislerine bağlanır ve cihazı kaydeder. Bağlanınca marquee başlatılır.
	 */
	private fun init() {
        val manager = try { GlyphMatrixManager.getInstance(applicationContext) } catch (e: Throwable) {
            UiNotifier.show(this, "GlyphMatrixManager getInstance hata: ${e.message}")
            return
        }
		glyphManager = manager
        try {
            manager.init(object : GlyphMatrixManager.Callback {
			override fun onServiceConnected(name: ComponentName) {
                    // Cihazı çöz ve grid ölçülerini ayarla
                    val cfg = GlyphDeviceResolver.resolve(this@GlyphToyService)
                    try { manager.register(cfg.deviceCode) } catch (e: Throwable) {
                        // Fallback dene
                        for (code in GlyphDeviceResolver.allFallbacks()) {
                            try { manager.register(code); break } catch (_: Throwable) {}
                        }
                    }
                    // Grid ölçülerini güncelle (statik sabitler yerine)
                    currentGridWidth = cfg.gridWidth
                    currentGridHeight = cfg.gridHeight
				marqueeText = NowPlayingStore.getText()?.let { TextSanitizer.sanitizeForGlyph(it) } ?: marqueeText
				lastText = marqueeText
				lastWidth = estimateTextWidthCells(lastText)
				scrollX = 25 + entryPadding
				startMarquee()
				AppSettings.setMatrixRunning(this@GlyphToyService, true)
			}
			override fun onServiceDisconnected(name: ComponentName) {}
            })
        } catch (e: Throwable) {
            UiNotifier.show(this, "Glyph servise bağlanma hata: ${e.message}")
            return
        }
	}

	/** Eğer init edilmediyse bağlanmayı tamamlayıp ardından verilen işlemi çalıştırır. */
	private fun ensureInit(after: () -> Unit) {
		val m = glyphManager
		if (m != null) {
			after(); return
		}
		val manager = GlyphMatrixManager.getInstance(applicationContext)
		glyphManager = manager
		manager.init(object : GlyphMatrixManager.Callback {
			override fun onServiceConnected(name: ComponentName) {
				manager.register(Glyph.DEVICE_23111)
				after()
			}
			override fun onServiceDisconnected(name: ComponentName) {}
		})
	}

	/** Kayan yazı döngüsü: metni soldan sağa kaydırır, metin değişince başa sarar. */
	private fun startMarquee() {
		isRunning = true
		handler.post(object : Runnable {
			override fun run() {
				if (!isRunning) return
				// Bildirimden gelen en güncel yazıyı al ve Türkçe karakterleri sadeleştir
				val raw = NowPlayingStore.getText() ?: marqueeText
				val text = TextSanitizer.sanitizeForGlyph(raw)
				if (text != lastText) {
					lastText = text
					lastWidth = estimateTextWidthCells(lastText)
					scrollX = 25 + entryPadding
                    loopCount = 0
				}

				val obj = GlyphMatrixObject.Builder()
					.setText(lastText)
					.setPosition(scrollX, 10)
					.setScale(100)
					.setBrightness(255)
					.build()

				val frame = GlyphMatrixFrame.Builder()
					.addTop(obj)
					.build(this@GlyphToyService)

				glyphManager?.setMatrixFrame(frame)

				scrollX -= 1
				val endX = -lastWidth - resetGap
				if (scrollX < endX) {
                    // Bir tam tur tamamlandı (başlangıçtan tamamen çıkışa)
                    loopCount += 1
                    if (loopCount >= 2) {
                        // İki turdan sonra otomatik kapat
                        isRunning = false
                        handler.removeCallbacksAndMessages(null)
                        try { glyphManager?.turnOff() } catch (_: Throwable) {}
                        try { glyphManager?.unInit() } catch (_: Throwable) {}
                        glyphManager = null
                        AppSettings.setMatrixRunning(this@GlyphToyService, false)
                        stopSelf()
                        return
                    } else {
                        // Yeni tura başla
                        scrollX = 25 + loopPadding
                    }
                }
				val delay = normalDelayMsFor(lastText)
				handler.postDelayed(this, delay.toLong())
			}
		})
	}

    private fun startDisc() {
		isDiscAnimating = true
		// Marquee varsa durdur
		isRunning = false
		handler.removeCallbacksAndMessages(null)
        renderAlbumArtRawToy()
	}

    /**
     * Albüm kapağını doğrudan Matrix Toy'a pixel array olarak basar (döndürmesiz, masksız).
     * Amaç: önce doğru gösterimi doğrulamak. Sonra efektler eklenebilir.
     */
    private fun renderAlbumArtRawToy(previewOnly: Boolean = false) {
        val art = com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore.getInfo()?.art
        if (art == null) {
            // Albüm görseli yoksa metne düş
            UiNotifier.show(this, "Albüm görseli bulunamadı")
            if (!previewOnly) startMarquee() else previewMarqueeOnce()
            return
        }
        val pixels = try {
            // Not: Bazı SDK sürümlerinde "scale" parametresi 0 olmalı (otomatik ölçeklendirme)
            // Ayrıca genişlik/yükseklik cihaz grid boyutlarıyla eşleşmeli
            GlyphMatrixUtils.convertToGlyphMatrix(
                art,
                /*width*/ currentGridWidth,
                /*height*/ currentGridHeight,
                /*scaleOrBrightness*/ 0,
                /*offsetOrThreshold*/ 128,
                /*extraLevel*/ 255,
                /*flipX*/ false,
                /*flipY*/ false
            )
        } catch (e: Throwable) { UiNotifier.show(this, "convertToGlyphMatrix hata: ${e.message}"); null } ?: return
        if (!previewOnly) {
            try {
                glyphManager?.setGlyphMatrixTimeout(false)
            } catch (e: Throwable) { UiNotifier.show(this, "setGlyphMatrixTimeout hata: ${e.message}") }
            try { glyphManager?.setMatrixFrame(pixels) } catch (e: Throwable) { UiNotifier.show(this, "setMatrixFrame hata: ${e.message}") }
        }
        // Preview'a gönder
        GlyphPreviewStore.update(currentGridWidth, currentGridHeight, pixels)
    }

    private fun renderSampleImagePreviewOnly() {
        val ctx = applicationContext
        val drawable = try { androidx.core.content.ContextCompat.getDrawable(ctx, com.efedonmez.nothingmatrixmusicdisc.R.mipmap.ic_launcher) } catch (_: Throwable) { null }
        val bitmap = drawable?.let { try { GlyphMatrixUtils.drawableToBitmap(it) } catch (_: Throwable) { null } }
        val pixels = bitmap?.let {
            try {
                GlyphMatrixUtils.convertToGlyphMatrix(
                    it,
                    /*width*/ currentGridWidth,
                    /*height*/ currentGridHeight,
                    /*scaleOrBrightness*/ 0,
                    /*offsetOrThreshold*/ 128,
                    /*extraLevel*/ 255,
                    /*flipX*/ false,
                    /*flipY*/ false
                )
            } catch (_: Throwable) { null }
        } ?: return
        GlyphPreviewStore.update(currentGridWidth, currentGridHeight, pixels)
    }

    private fun renderPixelDataPreviewOnly(intent: Intent?) {
        if (intent == null) return
        val raw = intent.getStringExtra(EXTRA_PIXEL_DATA)?.trim().orEmpty()
        if (raw.isEmpty()) return
        val total = currentGridWidth * currentGridHeight
        val data: IntArray = try {
            if (raw.startsWith("[")) {
                val arr = JSONArray(raw)
                IntArray(minOf(arr.length(), total)) { idx -> arr.optInt(idx, 0).coerceIn(0, 255) }
            } else {
                val parts = raw.split(',')
                IntArray(minOf(parts.size, total)) { idx -> parts[idx].trim().toIntOrNull()?.coerceIn(0, 255) ?: 0 }
            }
        } catch (_: Throwable) { return }
        val pixels = if (data.size == total) data else IntArray(total) { i -> if (i < data.size) data[i] else 0 }
        GlyphPreviewStore.update(currentGridWidth, currentGridHeight, pixels)
    }

    private fun previewMarqueeOnce() {
        val obj = GlyphMatrixObject.Builder()
            .setText(lastText)
            .setPosition(0, 10)
            .setScale(100)
            .setBrightness(255)
            .build()
        val frame = GlyphMatrixFrame.Builder()
            .addTop(obj)
            .build(this)
        val pixels = try { frame.render() } catch (_: Throwable) { null } ?: return
        GlyphPreviewStore.update(currentGridWidth, currentGridHeight, pixels)
    }

	private fun rotatePixels(src: IntArray, width: Int, height: Int, angleDeg: Float): IntArray {
		val dst = IntArray(src.size) { 0 }
		val angle = Math.toRadians((-angleDeg).toDouble())
		val cos = kotlin.math.cos(angle)
		val sin = kotlin.math.sin(angle)
		val cx = (width - 1) / 2.0
		val cy = (height - 1) / 2.0
		for (y in 0 until height) {
			for (x in 0 until width) {
				val dx = x - cx
				val dy = y - cy
				val sx = (cx + dx * cos - dy * sin).roundToInt()
				val sy = (cy + dx * sin + dy * cos).roundToInt()
				if (sx in 0 until width && sy in 0 until height) {
					dst[y * width + x] = src[sy * width + sx]
				} else {
					dst[y * width + x] = 0
				}
			}
		}
		return dst
	}

	// Basit şekiller: piksel dizisi üretip doğrudan matris kanalına gönderir
	private fun renderShapeLine() {
        val pixels = ShapeRenderer.drawLine(currentGridWidth, currentGridHeight, 0, 0, currentGridWidth - 1, currentGridHeight - 1, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	private fun renderShapeRect() {
        val pixels = ShapeRenderer.drawRect(currentGridWidth, currentGridHeight, 2, 2, currentGridWidth - 3, currentGridHeight - 3, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	private fun renderShapeCircle() {
        val radius = (minOf(currentGridWidth, currentGridHeight) / 2.2).toInt()
        val pixels = ShapeRenderer.drawCircle(currentGridWidth, currentGridHeight, currentGridWidth / 2, currentGridHeight / 2, radius, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	// Örnek görseli Glyph Matrix'te göster (uygulama kaynağından)
	private fun renderSampleImage() {
		val ctx = applicationContext
		val drawable = try {
			androidx.core.content.ContextCompat.getDrawable(ctx, com.efedonmez.nothingmatrixmusicdisc.R.mipmap.ic_launcher)
		} catch (_: Throwable) { null }
		if (drawable == null) return
		val bitmap = try { GlyphMatrixUtils.drawableToBitmap(drawable) } catch (_: Throwable) { null } ?: return
        val pixels = try {
            GlyphMatrixUtils.convertToGlyphMatrix(
                bitmap,
                /*width*/ currentGridWidth,
                /*height*/ currentGridHeight,
                /*scaleOrBrightness*/ 0,
                /*offsetOrThreshold*/ 128,
                /*extraLevel*/ 255,
                /*flipX*/ false,
                /*flipY*/ false
            )
        } catch (_: Throwable) { null } ?: return
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	/**
	 * Dışarıdan (ör. GlyphMatrixPaint) alınan pixel data'yı (JSON veya düz liste) matrise gönderir.
	 * Beklenen formatlar:
	 * - JSON array (ör: "[0, 255, 0, ...]") uzunluğu = GRID_WIDTH * GRID_HEIGHT
	 * - Düz metin virgülle ayrık: "0,255,0,..."
	 */
	private fun renderPixelDataFromIntent(intent: Intent?) {
		if (intent == null) return
        val raw = intent.getStringExtra(EXTRA_PIXEL_DATA)?.trim().orEmpty()
		if (raw.isEmpty()) return
        val total = currentGridWidth * currentGridHeight
		val data: IntArray = try {
			if (raw.startsWith("[")) {
				val arr = JSONArray(raw)
				IntArray(minOf(arr.length(), total)) { idx -> arr.optInt(idx, 0).coerceIn(0, 255) }
			} else {
				val parts = raw.split(',')
				IntArray(minOf(parts.size, total)) { idx -> parts[idx].trim().toIntOrNull()?.coerceIn(0, 255) ?: 0 }
			}
        } catch (e: Throwable) { UiNotifier.show(this, "Pixel data parse hata: ${e.message}"); return }
		// Gerekirse uzunluğu tamamla
		val pixels = if (data.size == total) data else IntArray(total) { i -> if (i < data.size) data[i] else 0 }
        try { glyphManager?.setMatrixFrame(pixels) } catch (e: Throwable) { UiNotifier.show(this, "setMatrixFrame hata: ${e.message}") }
        GlyphPreviewStore.update(currentGridWidth, currentGridHeight, pixels)
	}
}


