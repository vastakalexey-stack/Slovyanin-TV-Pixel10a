package ru.pixel10a.slovyanintv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

private val Night = Color(0xFF071A2E)
private val Charcoal = Color(0xFF102B49)
private val Panel = Color(0xFF183D63)
private val Gold = Color(0xFF8CCBFF)
private val Parchment = Color(0xFFE5F4FF)
private val Muted = Color(0xFFA9CBE8)
private val Line = Color(0xFF35658F)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val imported = mutableStateOf<List<Channel>>(emptyList())
    private val favorites = mutableStateListOf<String>()
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { loadFile(it) } }
    private val createBackup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? -> uri?.let { exportBackup(it) } }
    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { importBackup(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreData()
        importIncomingIntent(intent)
        if (imported.value.isEmpty()) {
            lifecycleScope.launch {
                try {
                    val initialChannels = withContext(Dispatchers.IO) {
                        val body = assets.open("slava67_combined.m3u").bufferedReader().use { it.readText() }
                        M3uParser.parse(body, "Мои присланные плейлисты")
                    }
                    if (imported.value.isEmpty() && initialChannels.isNotEmpty()) {
                        imported.value = initialChannels.distinctBy { it.url }
                        persistData()
                    }
                } catch (_: Exception) {
                    // The catalog can also be loaded manually from the in-app catalog screen.
                }
            }
        }
        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var showCatalogDialog by remember { mutableStateOf(false) }
            var showMenu by remember { mutableStateOf(false) }
            var showSearch by remember { mutableStateOf(false) }
            var search by remember { mutableStateOf("") }
            var showSettings by remember { mutableStateOf(false) }
            var showPlaylists by remember { mutableStateOf(false) }
            var showAbout by remember { mutableStateOf(false) }
            var showCinema by remember { mutableStateOf(false) }
            var cinemaChannels by remember { mutableStateOf<List<Channel>>(emptyList()) }
            var cinemaLoading by remember { mutableStateOf(false) }
            var cinemaError by remember { mutableStateOf("") }
            var cinemaSearch by remember { mutableStateOf("") }
            var cinemaCards by remember { mutableStateOf(false) }
            var channelForActions by remember { mutableStateOf<Channel?>(null) }
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var selectedGroup by remember { mutableStateOf("Все") }
            val rawChannels = imported.value.filter(ChannelPolicy::keep).map { it.copy(group = ChannelPolicy.displayGroup(it.group)) }
            val channels = ChannelPolicy.limitRegionalChannels(ChannelMerger.merge(rawChannels))
            val groups = listOf("Все", "Избранное") + channels.map { it.group }.distinct().sorted()
            val shown = channels.filter {
                (selectedGroup == "Все" || (selectedGroup == "Избранное" && it.url in favorites) || it.group == selectedGroup) &&
                    (selectedGroup != "Избранное" || it.url in favorites) &&
                    (search.isBlank() || it.name.contains(search, true) || it.group.contains(search, true))
            }
            val scope = rememberCoroutineScope()
            val loadCinemaCatalog: () -> Unit = {
                if (!cinemaLoading) {
                    cinemaLoading = true
                    cinemaError = ""
                    scope.launch {
                        try {
                            val sources = listOf(
                                "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/TV",
                                "https://iptv-org.github.io/iptv/index.m3u",
                                "https://iptv.org.ua/iptv/iptv.m3u",
                                "https://iptv.org.ua/iptv/ua.m3u",
                                "http://31.57.108.245:8080/Egorka.⅖_1.m3u",
                                "http://31.57.108.245:8080/playlist_ilookTV.m3u8",
                                "http://cdntv.online/hls/k9nmfxjv5wyr/playlist.m3u8"
                            )
                            val loaded = withContext(Dispatchers.IO) {
                                sources.mapIndexedNotNull { index, source ->
                                    runCatching {
                                        val body = URL(source).openConnection().apply {
                                            connectTimeout = 12000
                                            readTimeout = 25000
                                            setRequestProperty("User-Agent", "SlovyaninTV/1.4 Android")
                                        }.getInputStream().bufferedReader().use { it.readText() }
                                        M3uParser.parse(body, "Список пользователя ${index + 1}")
                                    }.getOrNull()
                                }.flatten()
                            }
                            val onlyUserSources = ChannelMerger.merge(loaded)
                                .filter(ChannelPolicy::keep)
                                .distinctBy { it.url }
                            cinemaChannels = onlyUserSources
                            if (onlyUserSources.isEmpty()) cinemaError = "Не удалось получить каналы из присланных списков. Проверьте доступность источников."
                        } catch (_: Exception) {
                            cinemaError = "Не удалось загрузить список. Проверьте интернет и попробуйте ещё раз."
                        } finally {
                            cinemaLoading = false
                        }
                    }
                }
            }

            MaterialTheme(colorScheme = darkColorScheme(
                primary = Gold, onPrimary = Night, secondary = Color(0xFF5BA9E8),
                background = Night, onBackground = Parchment, surface = Charcoal,
                onSurface = Parchment, surfaceVariant = Panel, onSurfaceVariant = Muted,
                outline = Line
            )) {
                Scaffold(
                    containerColor = Night,
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("ᚱ  СЛАВЯНИН ТВ  ᚱ", fontWeight = FontWeight.Black, color = Parchment)
                                    Text("ТЕЛЕВИДЕНИЕ • В ДУХЕ ТРАДИЦИЙ", style = MaterialTheme.typography.labelSmall, color = Gold)
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) search = "" }) {
                                    Icon(if (showSearch) Icons.Default.Close else Icons.Default.Search, "Поиск", tint = Gold)
                                }
                                Box {
                                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Меню", tint = Gold) }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Charcoal)) {
                                        DropdownMenuItem(text = { Text("Мои плейлисты", color = Parchment) }, leadingIcon = { Icon(Icons.Default.PlaylistPlay, null, tint = Gold) }, onClick = { showMenu = false; showSettings = false; showAbout = false; showPlaylists = true })
                                        DropdownMenuItem(text = { Text("Настройки", color = Parchment) }, leadingIcon = { Icon(Icons.Default.Settings, null, tint = Gold) }, onClick = { showMenu = false; showSettings = true; showAbout = false })
                                        DropdownMenuItem(text = { Text("О приложении", color = Parchment) }, leadingIcon = { Icon(Icons.Default.Info, null, tint = Gold) }, onClick = { showMenu = false; showAbout = true; showSettings = false })
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Charcoal)
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(12.dp))
                        if (showSearch) {
                            OutlinedTextField(
                                value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, label = { Text("Найти канал") },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Gold, unfocusedBorderColor = Line,
                                    focusedTextColor = Parchment, unfocusedTextColor = Parchment
                                )
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        if (showCinema) {
                            SlavicPanel(title = "КИНОЗАЛЫ • ОТКРЫТЫЕ ИСТОЧНИКИ") {
                                Text("Киноканалы с фильмами в эфире", color = Parchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                                Text("Это открытые телеканалы категории Movies, а не видеотека отдельных фильмов по запросу. Каждый поток запускается в штатном плеере.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { loadCinemaCatalog() }, enabled = !cinemaLoading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)) {
                                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text(if (cinemaLoading) "ЗАГРУЗКА…" else "ОБНОВИТЬ СПИСКИ")
                                    }
                                    Text("${cinemaChannels.size}", color = Gold, style = MaterialTheme.typography.labelMedium)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(selected = !cinemaCards, onClick = { cinemaCards = false }, label = { Text("Список") })
                                    FilterChip(selected = cinemaCards, onClick = { cinemaCards = true }, label = { Text("Карточки") })
                                }
                                OutlinedTextField(
                                    value = cinemaSearch, onValueChange = { cinemaSearch = it },
                                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                                    label = { Text("Поиск киноканала") },
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Line, focusedTextColor = Parchment, unfocusedTextColor = Parchment)
                                )
                                if (cinemaLoading) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Gold)
                                }
                                if (cinemaError.isNotBlank()) {
                                    Text(cinemaError, color = Color(0xFFFFB4A9))
                                }
                                val filteredCinema = cinemaChannels.filter { cinemaSearch.isBlank() || it.name.contains(cinemaSearch, true) || it.group.contains(cinemaSearch, true) }
                                if (!cinemaLoading && cinemaChannels.isEmpty() && cinemaError.isBlank()) {
                                    Text("Нажмите «Обновить список», чтобы загрузить открытые киноканалы.", color = Muted)
                                }
                                if (cinemaCards) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 145.dp),
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        gridItems(filteredCinema, key = { it.url }) { movieChannel ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java)
                                                        .putExtra("url", movieChannel.url)
                                                        .putExtra("name", movieChannel.name))
                                                },
                                                colors = CardDefaults.cardColors(containerColor = Panel),
                                                border = BorderStroke(1.dp, Line)
                                            ) {
                                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.Movie, null, tint = Gold, modifier = Modifier.size(26.dp))
                                                    Text(movieChannel.name, color = Parchment, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                    Text(movieChannel.group, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                    Icon(Icons.Default.PlayCircle, "Смотреть", tint = Gold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(filteredCinema, key = { it.url }) { movieChannel ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java)
                                                        .putExtra("url", movieChannel.url)
                                                        .putExtra("name", movieChannel.name))
                                                },
                                                colors = CardDefaults.cardColors(containerColor = Panel),
                                                border = BorderStroke(1.dp, Line)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Movie, null, tint = Gold)
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(movieChannel.name, color = Parchment, fontWeight = FontWeight.SemiBold)
                                                        Text(movieChannel.group, color = Muted, style = MaterialTheme.typography.labelSmall)
                                                    }
                                                    Icon(Icons.Default.PlayCircle, "Смотреть", tint = Gold)
                                                }
                                            }
                                        }
                                    }
                                }
                                Text("Загружаются только присланные тобой плейлисты. Недоступные источники пропускаются; спортивные каналы отсекаются фильтром приложения.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { showCinema = false; cinemaSearch = "" }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else if (showPlaylists) {
                            SlavicPanel(title = "МОИ ПЛЕЙЛИСТЫ") {
                                val playlistNames = rawChannels.map { it.playlist.ifBlank { "Без названия" } }.distinct()
                                if (playlistNames.isEmpty()) {
                                    Text("Пока нет добавленных плейлистов.", color = Muted)
                                } else {
                                    playlistNames.forEach { playlistName ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(playlistName, color = Parchment, fontWeight = FontWeight.SemiBold)
                                                Text("${rawChannels.count { it.playlist.ifBlank { "Без названия" } == playlistName }} канал(ов)", color = Muted, style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(onClick = {
                                                imported.value = imported.value.filterNot { it.playlist.ifBlank { "Без названия" } == playlistName }
                                                persistData()
                                            }) { Icon(Icons.Default.Delete, "Удалить плейлист", tint = Gold) }
                                        }
                                        HorizontalDivider(color = Line)
                                    }
                                }
                                TextButton(onClick = { showPlaylists = false }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else if (showSettings) {
                            SlavicPanel(title = "НАСТРОЙКИ") {
                                Text("Оформление: славянская ночная тема", color = Parchment)
                                Text("Палитра: угольный камень, тёплая бронза и пергамент", color = Muted)
                                Spacer(Modifier.height(8.dp))
                                Text("Плейлистов добавлено: ${channels.map { it.playlist }.distinct().size}", color = Gold)
                                Text("Каналов в каталоге: ${channels.size}", color = Parchment)
                                Spacer(Modifier.height(8.dp))
                                Text("РЕЗЕРВНАЯ КОПИЯ", color = Gold, fontWeight = FontWeight.Bold)
                                Text("Сохраните каналы и избранное в JSON-файл перед обновлением или переносом приложения.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                Button(onClick = { createBackup.launch("Slovyanin-TV-backup.json") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)) {
                                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("СОХРАНИТЬ РЕЗЕРВНУЮ КОПИЮ")
                                }
                                OutlinedButton(onClick = { openBackup.launch(arrayOf("application/json", "text/*", "*/*")) }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Gold), colors = ButtonDefaults.outlinedButtonColors(contentColor = Parchment)) {
                                    Icon(Icons.Default.Restore, null); Spacer(Modifier.width(8.dp)); Text("ВОССТАНОВИТЬ ИЗ КОПИИ")
                                }
                                Text("Восстановление добавляет каналы и избранное к текущим данным, не удаляя существующие записи.", color = Muted, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { showSettings = false }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else if (showAbout) {
                            SlavicPanel(title = "О СЛАВЯНИН ТВ") {
                                Text("Личный IPTV-плеер для собственных и разрешённых M3U/M3U8-источников.", color = Parchment)
                                Text("Приложение не содержит готовых нелицензионных списков каналов.", color = Muted)
                                TextButton(onClick = { showAbout = false }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    startActivity(Intent(this@MainActivity, CinemaActivity::class.java))
                                },
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Gold.copy(alpha = 0.65f)),
                                colors = CardDefaults.cardColors(containerColor = Panel)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(52.dp).background(Charcoal, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = "Кинотека", tint = Gold, modifier = Modifier.size(30.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("КИНОТЕКА", color = Parchment, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("Фильмы и подборка для просмотра", color = Muted, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Открыть кинотеку", tint = Gold)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("КАНАЛЫ", style = MaterialTheme.typography.headlineSmall, color = Parchment, fontWeight = FontWeight.Bold)
                                    Text("ᛟ  ВАШ МЕДИА-ОБЕРЕГ  ᛟ", style = MaterialTheme.typography.labelSmall, color = Gold)
                                }
                                TextButton(onClick = { selectedGroup = if (selectedGroup == "Избранное") "Все" else "Избранное" }) {
                                    Icon(Icons.Default.Favorite, null, tint = Gold)
                                    Text("Избранное", color = Parchment)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { openFile.launch(arrayOf("*/*")) }, border = BorderStroke(1.dp, Line), colors = ButtonDefaults.outlinedButtonColors(contentColor = Parchment), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.FileOpen, null, tint = Gold); Spacer(Modifier.width(6.dp)); Text("Импорт M3U")
                                }
                                Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AddLink, null); Spacer(Modifier.width(6.dp)); Text("Добавить ссылку")
                                }
                            }
                            OutlinedButton(onClick = { showCatalogDialog = true }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Gold), colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)) {
                                Icon(Icons.Default.LiveTv, null); Spacer(Modifier.width(8.dp)); Text("ОТКРЫТЬ КАТАЛОГ КАНАЛОВ")
                            }
                            Spacer(Modifier.height(12.dp))
                            if (channels.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(groups) { group ->
                                        FilterChip(
                                            selected = selectedGroup == group,
                                            onClick = { selectedGroup = group },
                                            label = { Text(group) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Gold, selectedLabelColor = Night,
                                                labelColor = Parchment, containerColor = Charcoal
                                            ),
                                            border = FilterChipDefaults.filterChipBorder(
                                                enabled = true, selected = selectedGroup == group,
                                                borderColor = Line, selectedBorderColor = Gold
                                            )
                                        )
                                    }
                                }
                            }
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = Panel)
                            if (error.isNotBlank()) Text(error, color = Color(0xFFFF8A80), modifier = Modifier.padding(8.dp))
                            if (shown.isEmpty()) {
                                Spacer(Modifier.height(40.dp))
                                SlavicPanel(title = "ᚦ  ПОКА НЕТ КАНАЛОВ  ᚦ") {
                                    Icon(Icons.Default.Tv, null, tint = Gold, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                                    Text("Добавьте свой плейлист M3U/M3U8 или импортируйте файл с телефона.", color = Parchment)
                                    Text("Ваши каналы появятся здесь.", color = Muted)
                                }
                            } else {
                                LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    items(shown, key = { it.url }) { channel ->
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, Line),
                                            colors = CardDefaults.cardColors(containerColor = Panel)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().combinedClickable(
                                                    onClick = {
                                                        startActivity(Intent(this@MainActivity, PlayerActivity::class.java).putExtra("url", channel.url).putExtra("name", channel.name).putExtra("tvgId", channel.tvgId).putExtra("catchup", channel.catchup).putExtra("catchupSource", channel.catchupSource).putExtra("catchupDays", channel.catchupDays).putStringArrayListExtra("streamUrls", ArrayList(channel.streams.map { it.url })).putStringArrayListExtra("streamLabels", ArrayList(channel.streams.map { it.label.ifBlank { "Поток" } })))
                                                    },
                                                    onLongClick = { channelForActions = channel }
                                                ).padding(13.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(42.dp).background(Charcoal, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PlayArrow, null, tint = Gold, modifier = Modifier.size(30.dp))
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(channel.name, color = Parchment, fontWeight = FontWeight.SemiBold)
                                                    Text(channel.group, color = Muted, style = MaterialTheme.typography.labelSmall)
                                                    if (channel.hasArchive) Text("Архив доступен", color = Gold, style = MaterialTheme.typography.labelSmall)
                                                }
                                                IconButton(onClick = { if (channel.url in favorites) favorites.remove(channel.url) else favorites.add(channel.url); persistData() }) {
                                                    Icon(Icons.Default.Favorite, null, tint = if (channel.url in favorites) Gold else Muted)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (showCatalogDialog) AlertDialog(
                    onDismissRequest = { showCatalogDialog = false },
                    containerColor = Charcoal,
                    titleContentColor = Parchment,
                    textContentColor = Muted,
                    title = { Text("КАТАЛОГ КАНАЛОВ") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Выберите открытый список. Каналы загружаются из внешнего источника; доступность зависит от вещателя и региона.")
                            CatalogSources.forEach { source ->
                                OutlinedButton(
                                    onClick = {
                                        showCatalogDialog = false
                                        loading = true
                                        error = ""
                                        scope.launch {
                                            try {
                                                val body = withContext(Dispatchers.IO) {
                                                    URL(source.url).openConnection().apply {
                                                        connectTimeout = 15000
                                                        readTimeout = 20000
                                                    }.getInputStream().bufferedReader().use { it.readText() }
                                                }
                                                val parsed = M3uParser.parse(body, source.name)
                                                if (parsed.isEmpty()) {
                                                    error = "В списке «${source.title}» не найдено каналов."
                                                } else {
                                                    imported.value = (imported.value.filterNot { it.playlist == source.name } + parsed).distinctBy { it.url }
                                                    persistData()
                                                    selectedGroup = "Все"
                                                }
                                            } catch (_: Exception) {
                                                error = "Не удалось загрузить «${source.title}». Проверьте интернет или попробуйте позже."
                                            }
                                            loading = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, Line),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Parchment)
                                ) {
                                    Column(Modifier.fillMaxWidth()) {
                                        Text(source.title, color = Gold, fontWeight = FontWeight.SemiBold)
                                        Text(source.description, color = Muted, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showCatalogDialog = false }) { Text("Закрыть", color = Gold) } }
                )
                channelForActions?.let { selectedChannel ->
                    AlertDialog(
                        onDismissRequest = { channelForActions = null },
                        containerColor = Charcoal,
                        titleContentColor = Parchment,
                        textContentColor = Muted,
                        title = { Text(selectedChannel.name) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Переместить канал в группу:")
                                channels.map { it.group }.distinct().sorted().forEach { targetGroup ->
                                    TextButton(onClick = {
                                        imported.value = imported.value.map { item ->
                                            if (item.url == selectedChannel.url) item.copy(group = targetGroup) else item
                                        }
                                        persistData()
                                        channelForActions = null
                                    }) { Text(targetGroup, color = Gold) }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                imported.value = imported.value.filterNot { it.url == selectedChannel.url }
                                favorites.remove(selectedChannel.url)
                                persistData()
                                channelForActions = null
                            }) { Text("Удалить канал", color = Color(0xFFFF8A80)) }
                        },
                        dismissButton = { TextButton(onClick = { channelForActions = null }) { Text("Отмена", color = Muted) } }
                    )
                }
                if (showDialog) AlertDialog(
                    onDismissRequest = { showDialog = false },
                    containerColor = Charcoal,
                    titleContentColor = Parchment,
                    textContentColor = Muted,
                    title = { Text("ДОБАВИТЬ ПЛЕЙЛИСТ") },
                    text = {
                        Column {
                            Text("Вставьте ссылку на свой M3U или M3U8-плейлист.")
                            OutlinedTextField(
                                value = url, onValueChange = { url = it }, label = { Text("https://…") }, singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Line, focusedTextColor = Parchment, unfocusedTextColor = Parchment)
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = url.startsWith("https://") || url.startsWith("http://"), onClick = {
                            val link = url.trim(); showDialog = false; loading = true; error = ""
                            scope.launch {
                                try {
                                    val body = withContext(Dispatchers.IO) { URL(link).openStream().bufferedReader().use { it.readText() } }
                                    val parsed = M3uParser.parse(body, link.substringAfterLast('/').ifBlank { "Онлайн-плейлист" })
                                    if (parsed.isEmpty()) error = "В плейлисте не найдено каналов."
                                    else { imported.value = (channels + parsed).distinctBy { it.url }; persistData() }
                                } catch (_: Exception) { error = "Не удалось загрузить плейлист. Проверьте ссылку и подключение." }
                                loading = false; url = ""
                            }
                        }) { Text("Загрузить", color = Gold) }
                    },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена", color = Muted) } }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importIncomingIntent(intent)
    }

    private fun importIncomingIntent(incoming: Intent?) {
        if (incoming == null) return
        val sharedUri = incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (sharedUri != null) {
            loadFile(sharedUri)
            return
        }
        val sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val viewUri = incoming.data
        val candidate = sharedText.ifBlank { viewUri?.toString().orEmpty() }
        if (candidate.isBlank()) return

        if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
            lifecycleScope.launch {
                try {
                    val body = withContext(Dispatchers.IO) {
                        URL(candidate).openConnection().apply {
                            connectTimeout = 15000
                            readTimeout = 30000
                        }.getInputStream().bufferedReader().use { it.readText() }
                    }
                    val importedChannels = M3uParser.parse(body, "AM Vision IPTV — импорт")
                    if (importedChannels.isNotEmpty()) {
                        imported.value = (imported.value + importedChannels).distinctBy { it.url }
                        persistData()
                    }
                } catch (_: Exception) {
                    // Unsupported or protected sources are not imported.
                }
            }
        } else if (candidate.startsWith("#EXTM3U") || candidate.contains("#EXTINF")) {
            val importedChannels = M3uParser.parse(candidate, "AM Vision IPTV — импорт")
            if (importedChannels.isNotEmpty()) {
                imported.value = (imported.value + importedChannels).distinctBy { it.url }
                persistData()
            }
        }
    }

    private fun channelToJson(channel: Channel) = JSONObject().apply {
        put("name", channel.name)
        put("url", channel.url)
        put("group", channel.group)
        put("logo", channel.logo ?: "")
        put("playlist", channel.playlist)
        put("tvgId", channel.tvgId)
        put("catchup", channel.catchup)
        put("catchupSource", channel.catchupSource)
        put("catchupDays", channel.catchupDays)
    }

    private fun channelFromJson(item: JSONObject) = Channel(
        name = item.optString("name"),
        url = item.optString("url"),
        group = item.optString("group", "Без категории"),
        logo = item.optString("logo").takeIf { it.isNotBlank() },
        playlist = item.optString("playlist"),
        tvgId = item.optString("tvgId"),
        catchup = item.optString("catchup"),
        catchupSource = item.optString("catchupSource"),
        catchupDays = item.optInt("catchupDays", 0)
    )

    private fun backupJson(): JSONObject {
        val root = JSONObject()
        val channelArray = JSONArray()
        imported.value.forEach { channelArray.put(channelToJson(it)) }
        val favoriteArray = JSONArray()
        favorites.forEach { favoriteArray.put(it) }
        root.put("backupVersion", 1)
        root.put("app", "Slovyanin TV")
        root.put("createdAt", System.currentTimeMillis())
        root.put("channels", channelArray)
        root.put("favorites", favoriteArray)
        return root
    }

    private fun exportBackup(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(backupJson().toString(2)) }
                ?: throw IllegalStateException("Не удалось открыть файл для записи")
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Не удалось сохранить копию: ${e.localizedMessage ?: "ошибка"}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun importBackup(uri: Uri) {
        try {
            val body = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: throw IllegalStateException("Не удалось прочитать файл")
            val root = JSONObject(body)
            if (root.optInt("backupVersion", 0) != 1 || root.optJSONArray("channels") == null) {
                throw IllegalArgumentException("Это не резервная копия Славянин ТВ")
            }
            val array = root.getJSONArray("channels")
            val restored = mutableListOf<Channel>()
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val channel = channelFromJson(item)
                if (channel.name.isNotBlank() && (channel.url.startsWith("http://") || channel.url.startsWith("https://"))) restored += channel
            }
            imported.value = (imported.value + restored).distinctBy { it.url }
            val savedFavorites = root.optJSONArray("favorites") ?: JSONArray()
            for (i in 0 until savedFavorites.length()) {
                val favorite = savedFavorites.optString(i)
                if (favorite.isNotBlank() && favorite !in favorites) favorites.add(favorite)
            }
            persistData()
            android.widget.Toast.makeText(this, "Копия восстановлена: добавлено каналов ${restored.size}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Не удалось восстановить копию: ${e.localizedMessage ?: "неверный файл"}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreData() {
        val prefs = getSharedPreferences("slovyanin_tv", MODE_PRIVATE)
        try {
            val root = JSONObject(prefs.getString("library", "{}") ?: "{}")
            val savedChannels = root.optJSONArray("channels") ?: JSONArray()
            val restored = mutableListOf<Channel>()
            for (i in 0 until savedChannels.length()) {
                val item = savedChannels.optJSONObject(i) ?: continue
                restored += channelFromJson(item)
            }
            imported.value = restored
            val savedFavorites = root.optJSONArray("favorites") ?: JSONArray()
            favorites.clear()
            for (i in 0 until savedFavorites.length()) favorites.add(savedFavorites.optString(i))
        } catch (_: Exception) {
            imported.value = emptyList()
            favorites.clear()
        }
    }

    private fun persistData() {
        val root = backupJson()
        getSharedPreferences("slovyanin_tv", MODE_PRIVATE).edit()
            .putString("library", root.toString()).apply()
    }

    private fun loadFile(uri: Uri) {
        try {
            val body = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            imported.value = (imported.value + M3uParser.parse(body, uri.lastPathSegment ?: "Файл")).distinctBy { it.url }; persistData()
        } catch (_: Exception) { }
    }
}

@Composable
private fun SlavicPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Line),
        colors = CardDefaults.cardColors(containerColor = Charcoal)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            Text("ᛉ  $title  ᛉ", color = Gold, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Line)
            content()
        })
    }
}
