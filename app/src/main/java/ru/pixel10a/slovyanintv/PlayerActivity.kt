package ru.pixel10a.slovyanintv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private val PlayerNight = Color(0xFF100F0D)
private val PlayerCharcoal = Color(0xFF1B1915)
private val PlayerPanel = Color(0xFF242019)
private val PlayerGold = Color(0xFFC49A55)
private val PlayerParchment = Color(0xFFE8D9BC)
private val PlayerMuted = Color(0xFF9C927F)
private val PlayerLine = Color(0xFF59452B)

class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var playbackError by androidx.compose.runtime.mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stream = intent.getStringExtra("url") ?: run { finish(); return }
        val title = intent.getStringExtra("name") ?: "Трансляция"

        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = true
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) playbackError = false
                }
            })
            exo.setMediaItem(MediaItem.fromUri(stream))
            exo.prepare()
            exo.playWhenReady = true
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = PlayerGold,
                onPrimary = PlayerNight,
                secondary = Color(0xFF9B7844),
                background = PlayerNight,
                onBackground = PlayerParchment,
                surface = PlayerCharcoal,
                onSurface = PlayerParchment,
                surfaceVariant = PlayerPanel,
                onSurfaceVariant = PlayerMuted,
                outline = PlayerLine
            )) {
                Scaffold(
                    containerColor = PlayerNight,
                    topBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(PlayerCharcoal)
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = PlayerGold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("СЛАВЯНИН ТВ", color = PlayerParchment, fontWeight = FontWeight.Black)
                                Text("ᛉ  МЕДИА-ОБЕРЕГ  ᛉ", color = PlayerGold, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = PlayerGold, modifier = Modifier.padding(end = 12.dp))
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, PlayerGold.copy(alpha = 0.65f)),
                            colors = CardDefaults.cardColors(containerColor = PlayerPanel)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(34.dp).background(PlayerCharcoal, RoundedCornerShape(9.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.LiveTv, null, tint = PlayerGold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(title, color = PlayerParchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("ПРЯМОЙ ЭФИР", color = PlayerGold, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Box(
                                    Modifier.fillMaxWidth()
                                        .background(Color.Black, RoundedCornerShape(12.dp))
                                ) {
                                    AndroidView(
                                        factory = { context ->
                                            PlayerView(context).apply {
                                                player = this@PlayerActivity.player
                                                useController = true
                                                controllerShowTimeoutMs = 4500
                                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                                setKeepContentOnPlayerReset(true)
                                            }
                                        },
                                        update = { it.player = player },
                                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PlayerLine),
                            colors = CardDefaults.cardColors(containerColor = PlayerCharcoal)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ᚦ  ИНФОРМАЦИЯ О ТРАНСЛЯЦИИ  ᚦ", color = PlayerGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                HorizontalDivider(color = PlayerLine)
                                if (playbackError) {
                                    Text("Не удалось запустить этот поток. Возможно, источник временно недоступен, ссылка устарела или требуется авторизация.", color = Color(0xFFFFB4A9), style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = {
                                        playbackError = false
                                        player?.seekTo(0)
                                        player?.prepare()
                                        player?.play()
                                    }) { Text("Повторить запуск", color = PlayerGold) }
                                } else {
                                    Text("Управление воспроизведением доступно на панели плеера. Нажмите на видео, чтобы показать или скрыть кнопки.", color = PlayerParchment, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("ᛟ  СЛАВЯНСКАЯ НОЧНАЯ ТЕМА  •  ЗОЛОТО И ПЕРГАМЕНТ", color = PlayerMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.release()
        player = null
    }
}
