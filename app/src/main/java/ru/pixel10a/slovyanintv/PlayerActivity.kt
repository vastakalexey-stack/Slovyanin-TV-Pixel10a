package ru.pixel10a.slovyanintv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stream = intent.getStringExtra("url") ?: run { finish(); return }
        val title = intent.getStringExtra("name") ?: "Трансляция"
        player = ExoPlayer.Builder(this).build().also { it.setMediaItem(MediaItem.fromUri(stream)); it.prepare(); it.playWhenReady = true }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(Modifier.fillMaxSize().padding(12.dp)) {
                    Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    AndroidView(factory = { context -> PlayerView(context).apply { player = this@PlayerActivity.player } }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                    Text("Если канал не запускается, источник может быть недоступен или требовать авторизацию.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
    override fun onStop() { super.onStop(); player?.release(); player = null }
}
