package com.efedonmez.nothingmatrixmusicdisc

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

class GlyphDemoActivity : ComponentActivity() {

    private var glyphManager: GlyphMatrixManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(color = MaterialTheme.colorScheme.background) {
                Text("Glyph Demo Çalışıyor")
            }
        }
        initGlyph()
    }

    private fun initGlyph() {
        val manager = GlyphMatrixManager.getInstance(applicationContext)
        glyphManager = manager
        manager.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName) {
                manager.register(Glyph.DEVICE_23111)
                showHelloOnce(manager)
            }
            override fun onServiceDisconnected(name: ComponentName) {}
        })
    }

    private fun showHelloOnce(manager: GlyphMatrixManager) {
        val hello = GlyphMatrixObject.Builder()
            .setText("DevPanda")
            .setPosition(4, 10)
            .setScale(100)
            .setBrightness(255)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(hello)
            .build(this)

        manager.setMatrixFrame(frame)
    }

    override fun onDestroy() {
        super.onDestroy()
        glyphManager?.turnOff()
        glyphManager?.unInit()
        glyphManager = null
    }
}

