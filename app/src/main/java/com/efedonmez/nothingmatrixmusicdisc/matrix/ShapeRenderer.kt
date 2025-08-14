package com.efedonmez.nothingmatrixmusicdisc.matrix

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Basit şekiller için int[] piksel dizisi üreten yardımcılar.
 *
 * Notlar:
 * - Dizinin boyutu cihazdan cihaza değişir. Çağıran taraf uygun GRID_WIDTH/GRID_HEIGHT değerlerini
 *   cihazınıza göre sağlamalıdır (örnek: 40x20). Aksi halde şekiller bozuk görünebilir.
 * - Piksel değeri olarak 0..255 parlaklık seviyesini kullanıyoruz. 0 kapalı, 255 açık gibi düşünebilirsiniz.
 */
object ShapeRenderer {

    /**
     * Satır-majör indeks: y * width + x
     */
    private fun indexOf(x: Int, y: Int, width: Int): Int = y * width + x

    /**
     * Ekranı temizler.
     */
    fun clear(width: Int, height: Int): IntArray = IntArray(width * height) { 0 }

    /**
     * Tek bir pikseli set eder (sınır kontrolleri ile).
     */
    fun setPixel(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, value: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        pixels[indexOf(x, y, width)] = value.coerceIn(0, 255)
    }

    /**
     * Düz çizgi (Bresenham benzeri basit yaklaşım).
     */
    fun drawLine(width: Int, height: Int, x0: Int, y0: Int, x1: Int, y1: Int, value: Int = 255): IntArray {
        val out = clear(width, height)
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            setPixel(out, width, height, x, y, value)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) {
                err += dy
                x += sx
            }
            if (e2 <= dx) {
                err += dx
                y += sy
            }
        }
        return out
    }

    /**
     * Dikdörtgen çiz (kenarlık).
     */
    fun drawRect(width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int, value: Int = 255): IntArray {
        val out = clear(width, height)
        val l = min(left, right)
        val r = max(left, right)
        val t = min(top, bottom)
        val b = max(top, bottom)
        for (x in l..r) {
            setPixel(out, width, height, x, t, value)
            setPixel(out, width, height, x, b, value)
        }
        for (y in t..b) {
            setPixel(out, width, height, l, y, value)
            setPixel(out, width, height, r, y, value)
        }
        return out
    }

    /**
     * Daire (kenarlık). Basit mesafe eşiği ile.
     */
    fun drawCircle(width: Int, height: Int, cx: Int, cy: Int, radius: Int, value: Int = 255): IntArray {
        val out = clear(width, height)
        val r2 = radius * radius
        for (y in 0 until height) {
            for (x in 0 until width) {
                val dx = x - cx
                val dy = y - cy
                val d2 = dx * dx + dy * dy
                // Kenarlık: yarıçap bandı içinde kalanları yakala
                if (abs(d2 - r2) <= max(1, radius / 2)) {
                    setPixel(out, width, height, x, y, value)
                }
            }
        }
        return out
    }

    /**
     * Basit dolu dikdörtgen.
     */
    fun fillRect(width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int, value: Int = 255): IntArray {
        val out = clear(width, height)
        val l = min(left, right)
        val r = max(left, right)
        val t = min(top, bottom)
        val b = max(top, bottom)
        for (y in t..b) {
            for (x in l..r) {
                setPixel(out, width, height, x, y, value)
            }
        }
        return out
    }
}


