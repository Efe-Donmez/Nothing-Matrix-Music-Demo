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
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

/**
 * GlyphToyService
 *
 * Nothing Glyph Toy servis entegrasyonu. Bu servis:
 * - Sistem Glyph servislerine bağlanır ve hedef cihazı kaydeder
 * - Bildirimden (NowPlaying) gelen şarkı bilgisini sanitize ederek kayan yazı olarak gösterir
 * - Basit şekilleri (çizgi/dikdörtgen/daire) tek dokunuşla render eder
 * - Başlat/Kapat eylemleri ile matris yönetimini sağlar
 */
class GlyphToyService : Service() {

	companion object {
		const val ACTION_START = "com.efedonmez.nothingmatrixmusicdisc.ACTION_START"
		const val ACTION_STOP = "com.efedonmez.nothingmatrixmusicdisc.ACTION_STOP"
		const val ACTION_SHOW_LINE = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_LINE"
		const val ACTION_SHOW_RECT = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_RECT"
		const val ACTION_SHOW_CIRCLE = "com.efedonmez.nothingmatrixmusicdisc.ACTION_SHOW_CIRCLE"

		// Cihaza göre düzenleyin (örnek boyutlar). Cihaz matrisine göre gerçek W/H giriniz.
		const val GRID_WIDTH = 40
		const val GRID_HEIGHT = 20

		fun startService(context: Context, action: String) {
			val i = Intent(context, GlyphToyService::class.java).setAction(action)
			context.startService(i)
		}
	}

	private var glyphManager: GlyphMatrixManager? = null
	private val handler = Handler(Looper.getMainLooper())
	private var isRunning = false
	private var scrollX = 40
	private var marqueeText: String = "Efe Donmez"
	private var lastText: String = marqueeText
	private var lastWidth: Int = 40
	private val resetGap = 0    // tamamen çıktıktan hemen sonra başlat
	private val entryPadding = 12 // ilk başlangıç biraz daha soldan
	private val loopPadding = 0   // tekrar döngüsünde minimum aralık
    private var loopCount: Int = 0  // tamamlanan tam geçiş sayısı

	/** Kayan yazı genişliğini yaklaşık hesaplar. */
	private fun estimateTextWidthCells(text: String): Int {
		return (text.length * 6).coerceAtLeast(8).coerceAtMost(120)
	}

	/** Metin uzunluğuna göre adım gecikmesi; 25..70 ms arası. */
	private fun normalDelayMsFor(text: String): Int {
		return (38 + text.length * 2).coerceIn(25, 70)
	}

	override fun onBind(intent: Intent?): IBinder? {
		init()
		return null
	}

	override fun onUnbind(intent: Intent?): Boolean {
		isRunning = false
		handler.removeCallbacksAndMessages(null)
		glyphManager?.turnOff()
		glyphManager?.unInit()
		glyphManager = null
		AppSettings.setMatrixRunning(this, false)
		return false
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		when (intent?.action) {
			ACTION_START -> ensureInit {
				AppSettings.setMatrixRunning(this, true)
				startMarquee()
			}
			ACTION_STOP -> {
				isRunning = false
				handler.removeCallbacksAndMessages(null)
				glyphManager?.turnOff()
				glyphManager?.unInit()
				glyphManager = null
				AppSettings.setMatrixRunning(this, false)
				stopSelf(startId)
			}
			ACTION_SHOW_LINE -> ensureInit { renderShapeLine() }
			ACTION_SHOW_RECT -> ensureInit { renderShapeRect() }
			ACTION_SHOW_CIRCLE -> ensureInit { renderShapeCircle() }
			else -> ensureInit { startMarquee() }
		}
		return START_NOT_STICKY
	}

	/**
	 * Glyph servislerine bağlanır ve cihazı kaydeder. Bağlanınca marquee başlatılır.
	 */
	private fun init() {
		val manager = GlyphMatrixManager.getInstance(applicationContext)
		glyphManager = manager
		manager.init(object : GlyphMatrixManager.Callback {
			override fun onServiceConnected(name: ComponentName) {
				manager.register(Glyph.DEVICE_23111)
				marqueeText = NowPlayingStore.getText()?.let { TextSanitizer.sanitizeForGlyph(it) } ?: marqueeText
				lastText = marqueeText
				lastWidth = estimateTextWidthCells(lastText)
				scrollX = 25 + entryPadding
				startMarquee()
				AppSettings.setMatrixRunning(this@GlyphToyService, true)
			}
			override fun onServiceDisconnected(name: ComponentName) {}
		})
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

	// Basit şekiller: piksel dizisi üretip doğrudan matris kanalına gönderir
	private fun renderShapeLine() {
		val pixels = ShapeRenderer.drawLine(GRID_WIDTH, GRID_HEIGHT, 0, 0, GRID_WIDTH - 1, GRID_HEIGHT - 1, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	private fun renderShapeRect() {
		val pixels = ShapeRenderer.drawRect(GRID_WIDTH, GRID_HEIGHT, 2, 2, GRID_WIDTH - 3, GRID_HEIGHT - 3, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}

	private fun renderShapeCircle() {
		val radius = (minOf(GRID_WIDTH, GRID_HEIGHT) / 2.2).toInt()
		val pixels = ShapeRenderer.drawCircle(GRID_WIDTH, GRID_HEIGHT, GRID_WIDTH / 2, GRID_HEIGHT / 2, radius, 255)
		try { glyphManager?.setMatrixFrame(pixels) } catch (_: Throwable) {}
	}
}


