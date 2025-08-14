package com.efedonmez.nothingmatrixmusicdisc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NotificationAccess
import com.efedonmez.nothingmatrixmusicdisc.settings.AppSettings
import com.efedonmez.nothingmatrixmusicdisc.ui.theme.NothingMatrixMusicDiscTheme
import android.content.Intent
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.res.stringResource
import com.efedonmez.nothingmatrixmusicdisc.toy.GlyphToyService
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextOverflow

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NothingMatrixMusicDiscTheme {
                val hasAccess = remember { mutableStateOf(NotificationAccess.isEnabled(this)) }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = stringResource(id = R.string.app_name)) },
                            actions = {
                                TextButton(onClick = {
                                    val i = Intent(this@MainActivity, GlyphToyService::class.java)
                                        .setAction(GlyphToyService.ACTION_START)
                                    startService(i)
                                }) { Text("Başlat") }
                                TextButton(onClick = {
                                    val i = Intent(this@MainActivity, GlyphToyService::class.java)
                                        .setAction(GlyphToyService.ACTION_STOP)
                                    startService(i)
                                }) { Text("Kapat") }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        val infoState = remember { mutableStateOf(com.efedonmez.nothingmatrixmusicdisc.nowplaying.NowPlayingStore.getInfo()) }
                        LaunchedEffect(Unit) {
                            // Basit polling (ileride Flow/Livedata yapılabilir)
                            while (true) {
                                infoState.value = NowPlayingStore.getInfo()
                                kotlinx.coroutines.delay(500)
                            }
                        }

                        ElevatedCard(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                val art = infoState.value?.art
                                if (art != null) {
                                    Image(bitmap = art.asImageBitmap(), contentDescription = null, modifier = Modifier.size(96.dp))
                                } else {
                                    Spacer(modifier = Modifier.size(96.dp))
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(text = infoState.value?.title ?: "Bilinmeyen Parça", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = infoState.value?.artist ?: "Bilinmeyen Sanatçı", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = if (infoState.value?.isPlaying == true) "Çalıyor" else "Duraklatıldı")
                                }
                            }
                            // İlerleme bilgisi varsa çubuk göster (şimdilik bilinmiyorsa gizli)
                            val pos = infoState.value?.positionMs
                            val dur = infoState.value?.durationMs
                            if (pos != null && dur != null && dur > 0) {
                                val progress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(progress = progress, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text(formatMs(pos))
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(formatMs(dur))
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Hızlı kontroller (Glyph)
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                val i = Intent(this@MainActivity, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_START)
                                startService(i)
                            }) { Text("Başlat") }
                            Spacer(modifier = Modifier.size(8.dp))
                            OutlinedButton(onClick = {
                                val i = Intent(this@MainActivity, GlyphToyService::class.java).setAction(GlyphToyService.ACTION_STOP)
                                startService(i)
                            }) { Text("Kapat") }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Bildirim erişimi durumu ve ayarı
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = if (hasAccess.value) "Bildirim erişimi açık" else "Bildirim erişimi kapalı")
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = { NotificationAccess.openSettings(this@MainActivity) }) { Text("Aç/Kapat") }
                        }

                        // App-mode ayarı
                        val appMode = remember { mutableStateOf(AppSettings.isAppModeEnabled(this@MainActivity)) }
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = appMode.value, onCheckedChange = {
                                appMode.value = it
                                AppSettings.setAppModeEnabled(this@MainActivity, it)
                            })
                            Text("Müzik değişiminde Matrix göster (app-mode)")
                        }
                    }
                }
                SideEffect {
                    if (!hasAccess.value) {
                        NotificationAccess.openSettings(this@MainActivity)
                    } else {
                        NotificationAccess.requestRebindIfPossible(this@MainActivity)
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NothingMatrixMusicDiscTheme {
        Greeting("Android")
    }
}