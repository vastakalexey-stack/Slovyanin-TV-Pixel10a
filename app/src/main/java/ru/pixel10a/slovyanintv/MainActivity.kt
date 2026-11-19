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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

private val Night = Color(0xFF100F0D)
private val Charcoal = Color(0xFF1B1915)
private val Panel = Color(0xFF242019)
private val Gold = Color(0xFFC49A55)
private val Parchment = Color(0xFFE8D9BC)
private val Muted = Color(0xFF9C927F)
private val Line = Color(0xFF59452B)

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val imported = mutableStateOf<List<Channel>>(emptyList())
    private val favorites = mutableStateListOf<String>()
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { loadFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreData()
        if (imported.value.isEmpty()) {
            lifecycleScope.launch {
                try {
                    val body = withContext(Dispatchers.IO) {
                        URL("https://iptv-org.github.io/iptv/languages/rus.m3u")
                            .openConnection().apply {
                                connectTimeout = 15000
                                readTimeout = 20000
                            }.getInputStream().bufferedReader().use { it.readText() }
                    }
                    val initialChannels = M3uParser.parse(body, "Каталог IPTV-org — русский язык")
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
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var selectedGroup by remember { mutableStateOf("Все") }
            val channels = imported.value
            val groups = listOf("Все", "Избранное") + channels.map { it.group }.distinct().sorted()
            val shown = channels.filter {
                (selectedGroup == "Все" || (selectedGroup == "Избранное" && it.url in favorites) || it.group == selectedGroup) &&
                    (selectedGroup != "Избранное" || it.url in favorites) &&
                    (search.isBlank() || it.name.contains(search, true) || it.group.contains(search, true))
            }
            val scope = rememberCoroutineScope()

            MaterialTheme(colorScheme = darkColorScheme(
                primary = Gold, onPrimary = Night, secondary = Color(0xFF9B7844),
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
                        if (showPlaylists) {
                            SlavicPanel(title = "МОИ ПЛЕЙЛИСТЫ") {
                                val playlistNames = channels.map { it.playlist.ifBlank { "Без названия" } }.distinct()
                                if (playlistNames.isEmpty()) {
                                    Text("Пока нет добавленных плейлистов.", color = Muted)
                                } else {
                                    playlistNames.forEach { playlistName ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(playlistName, color = Parchment, fontWeight = FontWeight.SemiBold)
                                                Text("${channels.count { it.playlist.ifBlank { "Без названия" } == playlistName }} канал(ов)", color = Muted, style = MaterialTheme.typography.labelSmall)
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
                                TextButton(onClick = { showSettings = false }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else if (showAbout) {
                            SlavicPanel(title = "О СЛАВЯНИН ТВ") {
                                Text("Личный IPTV-плеер для собственных и разрешённых M3U/M3U8-источников.", color = Parchment)
                                Text("Приложение не содержит готовых нелицензионных списков каналов.", color = Muted)
                                TextButton(onClick = { showAbout = false }) { Text("Назад к каналам", color = Gold) }
                            }
                        } else {
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
                                                Modifier.fillMaxWidth().clickable {
                                                    startActivity(Intent(this@MainActivity, PlayerActivity::class.java).putExtra("url", channel.url).putExtra("name", channel.name).putExtra("tvgId", channel.tvgId))
                                                }.padding(13.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(42.dp).background(Charcoal, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PlayArrow, null, tint = Gold, modifier = Modifier.size(30.dp))
                                                }
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text(channel.name, color = Parchment, fontWeight = FontWeight.SemiBold)
                                                    Text(channel.group, color = Muted, style = MaterialTheme.typography.labelSmall)
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

    private fun restoreData() {
        val prefs = getSharedPreferences("slovyanin_tv", MODE_PRIVATE)
        try {
            val root = JSONObject(prefs.getString("library", "{}") ?: "{}")
            val savedChannels = root.optJSONArray("channels") ?: JSONArray()
            val restored = mutableListOf<Channel>()
            for (i in 0 until savedChannels.length()) {
                val item = savedChannels.optJSONObject(i) ?: continue
                restored += Channel(
                    name = item.optString("name"),
                    url = item.optString("url"),
                    group = item.optString("group", "Без категории"),
                    logo = item.optString("logo").takeIf { it.isNotBlank() },
                    playlist = item.optString("playlist"),
                    tvgId = item.optString("tvgId")
                )
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
        val root = JSONObject()
        val channelArray = JSONArray()
        imported.value.forEach { channel ->
            channelArray.put(JSONObject().apply {
                put("name", channel.name)
                put("url", channel.url)
                put("group", channel.group)
                put("logo", channel.logo ?: "")
                put("playlist", channel.playlist)
                put("tvgId", channel.tvgId)
            })
        }
        val favoriteArray = JSONArray()
        favorites.forEach { favoriteArray.put(it) }
        root.put("channels", channelArray)
        root.put("favorites", favoriteArray)
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
