package com.efedonmez.nothingmatrixmusicdisc

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore
import com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphToyService
import com.efedonmez.nothingmatrixmusicdisc.ui.theme.NothingMatrixMusicDiscTheme
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.min

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NothingMatrixMusicDiscTheme {
                // 🔔 Uygulama açıldığında tüm izinleri kontrol et
                LaunchedEffect(Unit) {
                    com.efedonmez.nothingmatrixmusicdisc.util.PermissionHelper.checkAllPermissions(this@MainActivity)
                }
                
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                        Text(
                                    text = "Nothing Glyph Matrix",
                                    color = Color.White,
                                    fontWeight = FontWeight.Light
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    containerColor = Color.Black
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(paddingValues)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val infoState = remember { mutableStateOf(NowPlayingStore.getInfo()) }
                        val glyphShowArt = remember { mutableStateOf(AppSettings.isGlyphShowArt(this@MainActivity)) }
                        val glyphShowTitle = remember { mutableStateOf(AppSettings.isGlyphShowTitle(this@MainActivity)) }
                        
                        // Initialize mutually exclusive state
                        LaunchedEffect(Unit) {
                            if (glyphShowArt.value == glyphShowTitle.value) {
                                glyphShowArt.value = true
                                glyphShowTitle.value = false
                                AppSettings.setGlyphShowArt(this@MainActivity, true)
                                AppSettings.setGlyphShowTitle(this@MainActivity, false)
                            }
                        }
                        
                        // Observe music changes
                        LaunchedEffect(Unit) {
                            lifecycleScope.launchWhenStarted {
                                NowPlayingStore.infoFlow().collectLatest { info ->
                                    infoState.value = info
                                }
                            }
                        }

                        // Control Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                val isRunning = remember { mutableStateOf(AppSettings.isMatrixRunning(this@MainActivity)) }
                                
                                FilledTonalButton(
                                    onClick = {
                                        val currentlyRunning = AppSettings.isMatrixRunning(this@MainActivity)
                                        if (currentlyRunning) {
                                            // Kesin kapatma: Linear Matrix Controller ile
                                            AppSettings.setServiceDisabled(this@MainActivity, true)
                                            AppSettings.setMatrixRunning(this@MainActivity, false)
                                            startService(Intent(this@MainActivity, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_STOP))
                                            com.efedonmez.nothingmatrixmusicdisc.matrix.MatrixController.close(this@MainActivity)
                                            isRunning.value = false
                                        } else {
                                            // Başlatma: Service'i aktif et ve başlat
                                            AppSettings.setServiceDisabled(this@MainActivity, false)
                                            AppSettings.setMatrixRunning(this@MainActivity, true)
                                            val action = if (glyphShowArt.value) GlyphToyService.ACTION_SHOW_DISC else GlyphToyService.ACTION_START
                                            startService(Intent(this@MainActivity, GlyphToyService::class.java).setAction(action))
                                            isRunning.value = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isRunning.value) "KAPAT" else "BAŞLAT",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = if (isRunning.value) "● Glyph Matrix Aktif" else "○ Glyph Matrix Kapalı",
                                    color = if (isRunning.value) Color(0xFF00FF00) else Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Now Playing Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "ŞU AN ÇALIYOR",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val art = infoState.value?.art
                                    var rotation by remember { mutableStateOf(0f) }
                                    
                                    LaunchedEffect(infoState.value?.isPlaying) {
                                        while (infoState.value?.isPlaying == true) {
                                            rotation = (rotation + 2f) % 360f
                                            kotlinx.coroutines.delay(50)
                                        }
                                    }
                                    
                                    // Album Art with Vinyl Effect
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF333333))
                                    ) {
                                        if (art != null) {
                                            Image(
                                                bitmap = art.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .rotate(rotation)
                                            )
                                            // Vinyl center hole
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = infoState.value?.title ?: "Parça yok",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = infoState.value?.artist ?: "Sanatçı yok",
                                            color = Color.Gray,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (infoState.value?.isPlaying == true) "▶ Çalıyor" else "⏸ Duraklatıldı",
                                            color = if (infoState.value?.isPlaying == true) Color(0xFF00FF00) else Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                
                                // Progress bar
                                val pos = infoState.value?.positionMs
                                val dur = infoState.value?.durationMs
                                if (pos != null && dur != null && dur > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.White,
                                        trackColor = Color(0xFF333333)
                                    )
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(formatMs(pos), color = Color.Gray, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(formatMs(dur), color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                
                                // Matrix Preview
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "MATRIX ÖNİZLEME",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val previewState = remember { mutableStateOf(com.efedonmez.nothingmatrixmusicdisc.toy.GlyphPreviewStore.get()) }
                                LaunchedEffect(Unit) {
                                    while (true) {
                                        previewState.value = com.efedonmez.nothingmatrixmusicdisc.toy.GlyphPreviewStore.get()
                                        kotlinx.coroutines.delay(200)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(Color.Black, CircleShape)
                                        .align(Alignment.CenterHorizontally)
                                ) {
                                    val pf = previewState.value
                                    if (pf != null) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                                            val width = pf.width
                                            val height = pf.height
                                            val pixels = pf.pixels
                                            if (width > 0 && height > 0 && pixels.isNotEmpty()) {
                                                val cellW = size.width / width
                                                val cellH = size.height / height
                                                val rows = min(height, pixels.size / width)
                                                for (y in 0 until rows) {
                                                    for (x in 0 until width) {
                                                        val idx = y * width + x
                                                        if (idx >= pixels.size) continue
                                                        val v = pixels[idx].coerceIn(0, 255)
                                                        if (v > 0) {
                                                            drawRect(
                                                                color = Color.White.copy(alpha = v / 255f),
                                                                topLeft = androidx.compose.ui.geometry.Offset(x * cellW, y * cellH),
                                                                size = androidx.compose.ui.geometry.Size(cellW, cellH)
                                                            )
                                                        }
                                                    }
                                                }
                    }
                }
            }
        }
    }
}

                        // Settings Section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "AYARLAR",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Display Mode Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            // GÖRSEL moduna geç: Linear Matrix Controller ile
                                            glyphShowArt.value = true
                                            glyphShowTitle.value = false
                                            AppSettings.setGlyphShowArt(this@MainActivity, true)
                                            AppSettings.setGlyphShowTitle(this@MainActivity, false)
                                            
                                            // Anında güncelle (AppMatrix yolundan)
                                            com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(this@MainActivity)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (glyphShowArt.value) Color.White else Color(0xFF333333)
                                        )
                                    ) {
                                        Text(
                                            "GÖRSEL",
                                            color = if (glyphShowArt.value) Color.Black else Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    FilledTonalButton(
                                        onClick = {
                                            // METİN moduna geç: Linear Matrix Controller ile
                                            glyphShowTitle.value = true
                                            glyphShowArt.value = false
                                            AppSettings.setGlyphShowTitle(this@MainActivity, true)
                                            AppSettings.setGlyphShowArt(this@MainActivity, false)
                                            
                                            // Anında güncelle (AppMatrix yolundan)
                                            val text = NowPlayingStore.getText()
                                            if (!text.isNullOrBlank()) {
                                                com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixRenderer.renderText(this@MainActivity, text)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (glyphShowTitle.value) Color.White else Color(0xFF333333)
                                        )
                                    ) {
    Text(
                                            "METİN",
                                            color = if (glyphShowTitle.value) Color.Black else Color.Gray,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                
                                // Brightness & Contrast - Only show when GÖRSEL is selected
                                if (glyphShowArt.value) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    val brightState = remember { mutableStateOf(AppSettings.getMatrixBrightness(this@MainActivity)) }
                                    val contrastState = remember { mutableStateOf(AppSettings.getMatrixContrast(this@MainActivity)) }
                                    
                                    Text("Parlaklık: ${brightState.value}", color = Color.Gray, fontSize = 14.sp)
                                    Slider(
                                        value = brightState.value / 255f,
                                        onValueChange = {
                                            val v = (it * 255).toInt().coerceIn(0, 255)
                                            brightState.value = v
                                            AppSettings.setMatrixBrightness(this@MainActivity, v)
                                            // Anında güncelle (sadece görsel modda - AppMatrix)
                                            if (glyphShowArt.value) {
                                                com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(this@MainActivity)
                                            }
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF333333)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text("Kontrast: ${contrastState.value}", color = Color.Gray, fontSize = 14.sp)
                                    Slider(
                                        value = contrastState.value / 200f,
                                        onValueChange = {
                                            val v = (it * 200).toInt().coerceIn(0, 200)
                                            contrastState.value = v
                                            AppSettings.setMatrixContrast(this@MainActivity, v)
                                            // Anında güncelle (sadece görsel modda - AppMatrix)
                                            if (glyphShowArt.value) {
                                                com.efedonmez.nothingmatrixmusicdisc.appmatrix.AppMatrixImageRenderer.renderNowPlayingArt(this@MainActivity)
                                            }
                                        },
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF333333)
                                        )
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // İzin durumu ve ayarları
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        val notificationAccess = remember { mutableStateOf(com.efedonmez.nothingmatrixmusicdisc.nowplaying.NotificationAccess.isEnabled(this@MainActivity)) }
                                        val batteryOptimization = remember { mutableStateOf(com.efedonmez.nothingmatrixmusicdisc.util.PermissionHelper.isBatteryOptimizationDisabled(this@MainActivity)) }
                                        
                                        Text("İzinler", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "🔔 Bildirim: ${if (notificationAccess.value) "✅" else "❌"}",
                                            color = if (notificationAccess.value) Color(0xFF00FF00) else Color.Red,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "🔋 Pil: ${if (batteryOptimization.value) "✅" else "❌"}",
                                            color = if (batteryOptimization.value) Color(0xFF00FF00) else Color.Red,
                                            fontSize = 14.sp
                                        )
                                    }
                                    
                                    FilledTonalButton(
                                        onClick = {
                                            com.efedonmez.nothingmatrixmusicdisc.util.PermissionHelper.checkAllPermissions(this@MainActivity)
                                        }
                                    ) {
                                        Text("İZİNLER", fontSize = 12.sp)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Otomatik kapanma bilgisi
                                Text("Otomatik Kapanma: Resim 10sn, Yazı 5sn", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * GÖRSEL moduna geçiş yapar
     * 
     * Bu fonksiyon şunları yapar:
     * 1. Mevcut servisi tamamen durdurur
     * 2. Matrix ekranını temizler
     * 3. Ayarları GÖRSEL moduna çevirir
     * 4. Servisi GÖRSEL modunda yeniden başlatır
     */
    private fun switchToVisualMode() {
        // 1. ADIM: Mevcut servisi tamamen durdur
        stopAllMatrixServices()
        
        // 2. ADIM: Ayarları kaydet (GÖRSEL = açık, METİN = kapalı)
        AppSettings.setGlyphShowArt(this, true)
        AppSettings.setGlyphShowTitle(this, false)
        
        // 3. ADIM: 500ms bekle (servisin tamamen kapanması için)
        Handler(Looper.getMainLooper()).postDelayed({
            // 4. ADIM: Servisi GÖRSEL modunda yeniden başlat
            AppSettings.setServiceDisabled(this, false)
            startService(Intent(this, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_SHOW_DISC))
        }, 500)
    }

    /**
     * METİN moduna geçiş yapar
     * 
     * Bu fonksiyon şunları yapar:
     * 1. Mevcut servisi tamamen durdurur
     * 2. Matrix ekranını temizler
     * 3. Ayarları METİN moduna çevirir
     * 4. Servisi METİN modunda yeniden başlatır
     */
    private fun switchToTextMode() {
        // 1. ADIM: Mevcut servisi tamamen durdur
        stopAllMatrixServices()
        
        // 2. ADIM: Ayarları kaydet (METİN = açık, GÖRSEL = kapalı)
        AppSettings.setGlyphShowTitle(this, true)
        AppSettings.setGlyphShowArt(this, false)
        
        // 3. ADIM: 500ms bekle (servisin tamamen kapanması için)
        Handler(Looper.getMainLooper()).postDelayed({
            // 4. ADIM: Servisi METİN modunda yeniden başlat
            AppSettings.setServiceDisabled(this, false)
            startService(Intent(this, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_START))
        }, 500)
    }

    /**
     * Tüm Matrix servislerini tamamen durdurur ve temizler
     * 
     * Bu fonksiyon şunları yapar:
     * 1. Glyph Toy servisini durdurur
     * 2. App Matrix ekranını temizler
     * 3. Tüm ayarları "kapalı" yapar
     * 4. Matrix'in tamamen kapanmasını sağlar
     */
    private fun stopAllMatrixServices() {
        // Glyph Toy servisini durdur
        startService(Intent(this, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_STOP))
        
        // Linear Matrix Controller ile temizle
        com.efedonmez.nothingmatrixmusicdisc.matrix.MatrixController.close(this)
        
        // Durum ayarlarını kapat olarak işaretle
        AppSettings.setMatrixRunning(this, false)
        AppSettings.setServiceDisabled(this, true)
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

