package ru.pixel10a.slovyanintv

import android.os.Bundle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private val PlayerNight = Color(0xFF071A2E)
private val PlayerCharcoal = Color(0xFF102B49)
private val PlayerPanel = Color(0xFF183D63)
private val PlayerGold = Color(0xFF8CCBFF)
private val PlayerParchment = Color(0xFFE5F4FF)
private val PlayerMuted = Color(0xFFA9CBE8)
private val PlayerLine = Color(0xFF35658F)

class PlayerActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private val retryHandler = Handler(Looper.getMainLooper())
    private var retryAttempts = 0
    private var playbackError by mutableStateOf(false)
    private var epgLoading by mutableStateOf(false)
    private var epgError by mutableStateOf("")
    private var programs by mutableStateOf<List<TvProgram>>(emptyList())
    private var showEpg by mutableStateOf(false)
    private var showEpgSettings by mutableStateOf(false)
    private var epgUrl by mutableStateOf("")
    private var showArchiveButton by mutableStateOf(false)
    private var isArchivePlayback by mutableStateOf(false)
    private var showStreamPicker by mutableStateOf(false)
    private var selectedStreamIndex by mutableIntStateOf(0)
    private var streamUrls: List<String> = emptyList()
    private var streamLabels: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stream = intent.getStringExtra("url") ?: run { finish(); return }
        val title = intent.getStringExtra("name") ?: "Трансляция"
        val isCinemaContent = intent.getBooleanExtra("cinemaContent", false)
        streamUrls = intent.getStringArrayListExtra("streamUrls")?.filter { it.startsWith("http://") || it.startsWith("https://") }
            ?.distinct().orEmpty().ifEmpty { listOf(stream) }
        streamLabels = intent.getStringArrayListExtra("streamLabels").orEmpty()
        if (streamLabels.size != streamUrls.size) {
            streamLabels = streamUrls.indices.map { "Поток ${it + 1}" }
        }
        val tvgId = intent.getStringExtra("tvgId").orEmpty()
        val catchupMode = intent.getStringExtra("catchup").orEmpty()
        val catchupSource = intent.getStringExtra("catchupSource").orEmpty()
        val catchupDays = intent.getIntExtra("catchupDays", 0)
        showArchiveButton = !isCinemaContent && catchupMode.isNotBlank() && catchupSource.isNotBlank() && !catchupMode.equals("none", true) && !catchupMode.equals("disabled", true)
        val prefs = getSharedPreferences("slovyanin_tv", MODE_PRIVATE)
        epgUrl = prefs.getString("xmltv_url", "https://iptvx.one/epg/epg_lite.xml.gz") ?: "https://iptvx.one/epg/epg_lite.xml.gz"

        if (!isCinemaContent) {
            lifecycleScope.launch {
                epgLoading = true
                epgError = ""
                try {
                    programs = withContext(Dispatchers.IO) {
                        XmlTvParser.loadFromSources(
                            listOf(epgUrl, "https://epg.klimedia.pro/epg.xml"),
                            tvgId,
                            title
                        )
                    }
                    if (programs.isEmpty()) {
                        epgError = "Для этого канала не найдено расписание. Проверьте tvg-id или источник XMLTV."
                    }
                } catch (_: Exception) {
                    epgError = "Не удалось автоматически загрузить телепрограмму. Проверьте подключение или источник XMLTV."
                } finally {
                    epgLoading = false
                }
            }
        }

        player = ExoPlayer.Builder(this).build().also { exo ->
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (retryAttempts < 2) {
                        val delayMs = 1200L * (retryAttempts + 1)
                        retryAttempts++
                        playbackError = false
                        retryHandler.postDelayed({
                            player?.let { activePlayer ->
                                activePlayer.prepare()
                                activePlayer.play()
                            }
                        }, delayMs)
                    } else if (!isArchivePlayback && selectedStreamIndex < streamUrls.lastIndex) {
                        selectedStreamIndex++
                        retryAttempts = 0
                        playbackError = false
                        val nextUrl = streamUrls[selectedStreamIndex]
                        retryHandler.postDelayed({
                            player?.let { activePlayer ->
                                activePlayer.setMediaItem(MediaItem.fromUri(nextUrl))
                                activePlayer.prepare()
                                activePlayer.play()
                            }
                        }, 500L)
                    } else {
                        playbackError = true
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        retryAttempts = 0
                        playbackError = false
                    }
                }
            })
            exo.setMediaItem(MediaItem.fromUri(stream))
            exo.prepare()
            exo.playWhenReady = true
        }

        setContent {
            val scope = rememberCoroutineScope()
            var screenMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
            MaterialTheme(colorScheme = darkColorScheme(
                primary = PlayerGold, onPrimary = PlayerNight, secondary = Color(0xFF5BA9E8),
                background = PlayerNight, onBackground = PlayerParchment, surface = PlayerCharcoal,
                onSurface = PlayerParchment, surfaceVariant = PlayerPanel,
                onSurfaceVariant = PlayerMuted, outline = PlayerLine
            )) {
                Scaffold(
                    containerColor = PlayerNight,
                    topBar = {
                        Row(
                            Modifier.fillMaxWidth().background(PlayerCharcoal).padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = PlayerGold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("СЛАВЯНИН ТВ", color = PlayerParchment, fontWeight = FontWeight.Black)
                                Text("ᛉ  МЕДИА-ОБЕРЕГ  ᛉ", color = PlayerGold, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.LiveTv, null, tint = PlayerGold, modifier = Modifier.padding(end = 12.dp))
                        }
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, PlayerGold.copy(alpha = 0.65f)),
                            colors = CardDefaults.cardColors(containerColor = PlayerPanel)
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(34.dp).background(PlayerCharcoal, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.LiveTv, null, tint = PlayerGold)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(title, color = PlayerParchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(if (isArchivePlayback) "АРХИВНАЯ ЗАПИСЬ" else "ПРЯМОЙ ЭФИР", color = PlayerGold, style = MaterialTheme.typography.labelSmall)
                                        if (streamUrls.size > 1 && !isArchivePlayback) {
                                            Text("АКТИВЕН: ${streamLabels.getOrElse(selectedStreamIndex) { "Поток ${selectedStreamIndex + 1}" }}", color = PlayerMuted, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    if (streamUrls.size > 1 && !isArchivePlayback) {
                                        IconButton(onClick = { showStreamPicker = true }) {
                                            Icon(Icons.Default.SwapHoriz, contentDescription = "Сменить поток", tint = PlayerGold)
                                        }
                                    }
                                }
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
                                    update = { view ->
                                        view.player = player
                                        view.resizeMode = screenMode
                                    },
                                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black, RoundedCornerShape(12.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Вписать" to AspectRatioFrameLayout.RESIZE_MODE_FIT,
                                        "Заполнить" to AspectRatioFrameLayout.RESIZE_MODE_FILL,
                                        "Увеличить" to AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    ).forEach { (label, mode) ->
                                        val selected = screenMode == mode
                                        OutlinedButton(
                                            onClick = { screenMode = mode },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                            border = BorderStroke(1.dp, if (selected) PlayerGold else PlayerLine),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (selected) PlayerGold else PlayerParchment
                                            )
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }

                        if (showArchiveButton) {
                            OutlinedButton(
                                onClick = { openArchivePicker(stream, catchupMode, catchupSource, catchupDays) },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, PlayerGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayerGold)
                            ) {
                                Text("АРХИВ ПЕРЕДАЧ")
                            }
                            if (isArchivePlayback) {
                                OutlinedButton(
                                    onClick = {
                                        retryAttempts = 0
                                        playbackError = false
                                        isArchivePlayback = false
                                        player?.setMediaItem(MediaItem.fromUri(stream))
                                        player?.prepare()
                                        player?.play()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, PlayerLine),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayerParchment)
                                ) {
                                    Text("ВЕРНУТЬСЯ К ПРЯМОМУ ЭФИРУ")
                                }
                            }
                        }

                        if (!isCinemaContent) {
                            OutlinedButton(
                                onClick = {
                                    showEpg = true
                                    if (programs.isEmpty() && !epgLoading && epgUrl.isNotBlank()) {
                                        epgLoading = true
                                        epgError = ""
                                        scope.launch {
                                            try {
                                                programs = withContext(Dispatchers.IO) {
                                                    XmlTvParser.loadFromSources(listOf(epgUrl, "https://epg.klimedia.pro/epg.xml"), tvgId, title)
                                                }
                                                if (programs.isEmpty()) epgError = "Для этого канала в телепрограмме передач не найдено."
                                            } catch (_: Exception) {
                                                epgError = "Не удалось загрузить XMLTV. Проверьте источник и подключение к интернету."
                                            } finally {
                                                epgLoading = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, PlayerGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayerGold)
                            ) {
                                Icon(Icons.Default.LiveTv, null)
                                Spacer(Modifier.width(8.dp))
                                Text("ПРОГРАММА ПЕРЕДАЧ")
                            }
                        }

                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, PlayerLine),
                            colors = CardDefaults.cardColors(containerColor = PlayerCharcoal)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ᚦ  ИНФОРМАЦИЯ О ТРАНСЛЯЦИИ  ᚦ", color = PlayerGold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                HorizontalDivider(color = PlayerLine)
                                if (playbackError) {
                                    Text("Не удалось запустить этот поток. Возможно, источник временно недоступен, ссылка устарела или требуется авторизация.", color = Color(0xFFFFB4A9))
                                    TextButton(onClick = { retryAttempts = 0; playbackError = false; player?.seekTo(0); player?.prepare(); player?.play() }) {
                                        Text("Повторить запуск", color = PlayerGold)
                                    }
                                } else {
                                    Text("Управление воспроизведением доступно на панели плеера. Нажмите на видео, чтобы показать или скрыть кнопки.", color = PlayerParchment)
                                }
                                Text("ᛟ  СЛАВЯНСКАЯ НОЧНАЯ ТЕМА  •  ЗОЛОТО И ПЕРГАМЕНТ", color = PlayerMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                if (showStreamPicker) AlertDialog(
                    onDismissRequest = { showStreamPicker = false },
                    containerColor = PlayerCharcoal,
                    titleContentColor = PlayerParchment,
                    textContentColor = PlayerMuted,
                    title = { Text("ВЫБОР ПОТОКА") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            streamUrls.forEachIndexed { index, url ->
                                val selected = index == selectedStreamIndex
                                OutlinedButton(
                                    onClick = {
                                        selectedStreamIndex = index
                                        showStreamPicker = false
                                        retryHandler.removeCallbacksAndMessages(null)
                                        retryAttempts = 0
                                        playbackError = false
                                        isArchivePlayback = false
                                        player?.setMediaItem(MediaItem.fromUri(url))
                                        player?.prepare()
                                        player?.play()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, if (selected) PlayerGold else PlayerLine),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selected) PlayerGold else PlayerParchment)
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(streamLabels.getOrElse(index) { "Поток ${index + 1}" }, modifier = Modifier.weight(1f))
                                        if (selected) Text("✓", color = PlayerGold, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStreamPicker = false }) { Text("ЗАКРЫТЬ", color = PlayerGold) }
                    }
                )

                if (showEpg) AlertDialog(
                    onDismissRequest = { showEpg = false },
                    containerColor = PlayerCharcoal,
                    titleContentColor = PlayerParchment,
                    textContentColor = PlayerMuted,
                    title = { Text("ᛉ  ПРОГРАММА ПЕРЕДАЧ  ᛉ") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (epgLoading) {
                                LinearProgressIndicator(Modifier.fillMaxWidth(), color = PlayerGold)
                                Text("Загружаем телепрограмму…")
                            } else if (epgError.isNotBlank()) {
                                Text(epgError, color = Color(0xFFFFB4A9))
                            } else if (programs.isNotEmpty()) {
                                val now = System.currentTimeMillis()
                                val current = programs.firstOrNull { now in it.start..it.stop }
                                if (current != null) {
                                    Text("СЕЙЧАС", color = PlayerGold, fontWeight = FontWeight.Bold)
                                    Text(current.title, color = PlayerParchment, fontWeight = FontWeight.SemiBold)
                                    Text("${clock(current.start)}–${clock(current.stop)}", color = PlayerMuted)
                                    if (current.description.isNotBlank()) Text(current.description, color = PlayerMuted)
                                    HorizontalDivider(color = PlayerLine)
                                }
                                Text("ДАЛЕЕ ПО ПРОГРАММЕ", color = PlayerGold, fontWeight = FontWeight.Bold)
                                LazyColumn(Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(programs.filter { it.start > now || now in it.start..it.stop }.take(12)) { item ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text("${clock(item.start)}–${clock(item.stop)}  •  ${item.title}", color = PlayerParchment, fontWeight = FontWeight.Medium)
                                            if (item.description.isNotBlank()) Text(item.description, color = PlayerMuted, style = MaterialTheme.typography.bodySmall)
                                        }
                                        HorizontalDivider(color = PlayerLine)
                                    }
                                }
                            } else {
                                Text("Источник телепрограммы ещё не задан. Добавьте ссылку на XMLTV, чтобы видеть расписание.", color = PlayerParchment)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEpg = false; showEpgSettings = true }) { Text("Источник XMLTV", color = PlayerGold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEpg = false }) { Text("Закрыть", color = PlayerMuted) }
                    }
                )

                if (showEpgSettings) AlertDialog(
                    onDismissRequest = { showEpgSettings = false },
                    containerColor = PlayerCharcoal,
                    titleContentColor = PlayerParchment,
                    textContentColor = PlayerMuted,
                    title = { Text("ИСТОЧНИК ТЕЛЕПРОГРАММЫ") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Укажите ссылку на файл XMLTV (.xml, .xml.gz). Обычно её предоставляет IPTV-провайдер.")
                            OutlinedTextField(
                                value = epgUrl,
                                onValueChange = { epgUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("https://…/epg.xml или .xml.gz") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PlayerGold, unfocusedBorderColor = PlayerLine,
                                    focusedTextColor = PlayerParchment, unfocusedTextColor = PlayerParchment
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            prefs.edit().putString("xmltv_url", epgUrl.trim()).apply()
                            programs = emptyList()
                            epgError = ""
                            showEpgSettings = false
                            showEpg = true
                            if (epgUrl.isNotBlank()) {
                                epgLoading = true
                                scope.launch {
                                    try {
                                        programs = withContext(Dispatchers.IO) { XmlTvParser.load(epgUrl.trim(), tvgId, title) }
                                        if (programs.isEmpty()) epgError = "В XMLTV не найдено расписание для этого канала."
                                    } catch (_: Exception) {
                                        epgError = "Не удалось загрузить XMLTV. Проверьте ссылку."
                                    }
                                    epgLoading = false
                                }
                            }
                        }) { Text("Сохранить и загрузить", color = PlayerGold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEpgSettings = false }) { Text("Отмена", color = PlayerMuted) }
                    }
                )
            }
        }
    }

    private fun openArchivePicker(
        liveUrl: String,
        mode: String,
        source: String,
        availableDays: Int
    ) {
        val now = Calendar.getInstance()
        val earliest = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(availableDays.takeIf { it > 0 } ?: 7))
        }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        selected.set(Calendar.HOUR_OF_DAY, hour)
                        selected.set(Calendar.MINUTE, minute)
                        selected.set(Calendar.SECOND, 0)
                        selected.set(Calendar.MILLISECOND, 0)
                        if (selected.timeInMillis >= System.currentTimeMillis()) {
                            Toast.makeText(this, "Выберите время в прошлом", Toast.LENGTH_SHORT).show()
                            return@TimePickerDialog
                        }
                        val archiveUrl = CatchupUrlBuilder.build(
                            liveUrl = liveUrl,
                            mode = mode,
                            source = source,
                            startMillis = selected.timeInMillis
                        )
                        if (archiveUrl == null) {
                            Toast.makeText(this, "Не удалось сформировать ссылку архива для этого канала", Toast.LENGTH_LONG).show()
                            return@TimePickerDialog
                        }
                        retryHandler.removeCallbacksAndMessages(null)
                        retryAttempts = 0
                        playbackError = false
                        isArchivePlayback = true
                        player?.setMediaItem(MediaItem.fromUri(archiveUrl))
                        player?.prepare()
                        player?.play()
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                ).show()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
            datePicker.minDate = earliest.timeInMillis
        }.show()
    }

    override fun onStop() {
        super.onStop()
        retryHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }

    private fun clock(timestamp: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
