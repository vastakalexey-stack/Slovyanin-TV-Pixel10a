package ru.pixel10a.slovyanintv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

class SplashActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var openedMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        exoPlayer.setMediaItem(
            MediaItem.fromUri("android.resource://$packageName/${R.raw.intro}")
        )
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) openMain()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                openMain()
            }
        })

        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        PlayerView(context).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            player = exoPlayer
                        }
                    }
                )
            }
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun openMain() {
        if (openedMain || isFinishing) return
        openedMain = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
