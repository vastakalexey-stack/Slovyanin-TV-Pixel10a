package ru.pixel10a.slovyanintv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CinemaNight = Color(0xFF071A2E)
private val CinemaPanel = Color(0xFF102B49)
private val CinemaGold = Color(0xFF8CCBFF)
private val CinemaText = Color(0xFFE5F4FF)
private val CinemaMuted = Color(0xFFA9CBE8)

@OptIn(ExperimentalMaterial3Api::class)
class CinemaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var films by remember { mutableStateOf<List<Channel>>(emptyList()) }
            var query by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var cards by remember { mutableStateOf(true) }
            val scope = rememberCoroutineScope()

            fun loadCinema() {
                if (loading) return
                loading = true
                error = ""
                scope.launch {
                    try {
                        val loaded = withContext(Dispatchers.IO) {
                            val body = assets.open("slava67_combined.m3u").bufferedReader().use { it.readText() }
                            val parsed = M3uParser.parse(body, "Мои присланные плейлисты — Кинотека")
                                .map { it.copy(name = it.name.substringBefore("|").trim()) }
                            val allowedGroups = setOf(
                                "кинозалы", "kino zal", "kino zal (vpn)", "4k kino zal (vpn)",
                                "kino+📺", "фильмы-vpn"
                            )
                            ChannelMerger.merge(parsed)
                                .filter { channel ->
                                    channel.group.trim().lowercase() in allowedGroups &&
                                        ChannelPolicy.keep(channel)
                                }
                                .distinctBy { it.url }
                        }
                        films = loaded
                        if (loaded.isEmpty()) error = "В сохранённом пользовательском плейлисте не найдены фильмы."
                    } catch (_: Exception) {
                        error = "Не удалось прочитать сохранённый плейлист."
                    } finally {
                        loading = false
                    }
                }
            }

            val filtered = films.filter {
                query.isBlank() || it.name.contains(query, true) || it.group.contains(query, true)
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = CinemaGold,
                    background = CinemaNight,
                    surface = CinemaPanel,
                    onSurface = CinemaText
                )
            ) {
                Scaffold(
                    containerColor = CinemaNight,
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("ᚱ  КИНОТЕКА  ᚱ", color = CinemaText, fontWeight = FontWeight.Black)
                                    Text("ВАШИ ПЛЕЙЛИСТЫ • КИНОЗАЛЫ", color = CinemaGold, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.Default.ArrowBack, "Назад", tint = CinemaGold)
                                }
                            },
                            actions = {
                                IconButton(onClick = { loadCinema() }, enabled = !loading) {
                                    Icon(Icons.Default.Refresh, "Обновить", tint = CinemaGold)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaPanel)
                        )
                    }
                ) { padding ->
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Поиск по названию") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = CinemaGold) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CinemaGold,
                                unfocusedBorderColor = CinemaMuted,
                                focusedTextColor = CinemaText,
                                unfocusedTextColor = CinemaText
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(selected = cards, onClick = { cards = true }, label = { Text("Карточки") })
                            FilterChip(selected = !cards, onClick = { cards = false }, label = { Text("Список") })
                            Spacer(Modifier.weight(1f))
                            Text("${filtered.size} фильмов", color = CinemaGold, style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "Только материалы из твоего объединённого плейлиста. Внешний каталог Internet Archive не используется.",
                            color = CinemaMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CinemaGold)
                        if (error.isNotBlank()) Text(error, color = Color(0xFFFFB4A9))
                        if (films.isEmpty() && !loading) {
                            LaunchedEffect(Unit) { loadCinema() }
                        }
                        if (cards) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 145.dp),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                gridItems(filtered, key = { it.url }) { film ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { openFilm(film) },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, Color(0xFF35658F)),
                                        colors = CardDefaults.cardColors(containerColor = CinemaPanel)
                                    ) {
                                        Column {
                                            if (!film.logo.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = film.logo,
                                                    contentDescription = film.name,
                                                    modifier = Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF183D63)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF183D63)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Movie, null, tint = CinemaGold, modifier = Modifier.size(42.dp))
                                                }
                                            }
                                            Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(film.name, color = CinemaText, fontWeight = FontWeight.SemiBold, maxLines = 3)
                                                Text(ChannelPolicy.displayGroup(film.group), color = CinemaMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.PlayCircle, null, tint = CinemaGold, modifier = Modifier.size(18.dp))
                                                    Text(" Смотреть", color = CinemaGold, style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filtered, key = { it.url }) { film ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { openFilm(film) },
                                        colors = CardDefaults.cardColors(containerColor = CinemaPanel),
                                        border = BorderStroke(1.dp, Color(0xFF35658F))
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!film.logo.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = film.logo,
                                                    contentDescription = film.name,
                                                    modifier = Modifier.size(62.dp, 48.dp),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(Icons.Default.Movie, null, tint = CinemaGold, modifier = Modifier.size(38.dp))
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(film.name, color = CinemaText, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                                Text(ChannelPolicy.displayGroup(film.group), color = CinemaMuted, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Icon(Icons.Default.PlayCircle, "Смотреть", tint = CinemaGold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun openFilm(film: Channel) {
        val urls = film.streams.map { it.url }.distinct()
        val labels = film.streams.map { it.label }
        startActivity(
            Intent(this, PlayerActivity::class.java)
                .putExtra("url", film.url)
                .putExtra("name", film.name)
                .putExtra("cinemaContent", true)
                .putStringArrayListExtra("streamUrls", ArrayList(urls))
                .putStringArrayListExtra("streamLabels", ArrayList(labels))
                .putExtra("tvgId", film.tvgId)
                .putExtra("catchup", film.catchup)
                .putExtra("catchupSource", film.catchupSource)
                .putExtra("catchupDays", film.catchupDays)
        )
    }
}
