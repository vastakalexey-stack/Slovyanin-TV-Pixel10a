package ru.pixel10a.slovyanintv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

private val C_NIGHT = Color(0xFF071A2E)
private val C_PANEL = Color(0xFF102B49)
private val C_BLUE = Color(0xFF8CCBFF)
private val C_TEXT = Color(0xFFE5F4FF)
private val C_MUTED = Color(0xFFA9CBE8)

private data class ArchiveFilm(val id: String, val title: String, val year: String) {
    val poster: String get() = "https://archive.org/services/img/" + id
}

@OptIn(ExperimentalMaterial3Api::class)
class CinemaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var films by remember { mutableStateOf<List<ArchiveFilm>>(emptyList()) }
            var query by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var page by remember { mutableIntStateOf(1) }
            var hasMore by remember { mutableStateOf(true) }
            val gridState = rememberLazyGridState()
            val scope = rememberCoroutineScope()
            fun load(reset: Boolean) {
                if (loading) return
                loading = true
                error = ""
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { searchArchiveFilms(query, if (reset) 1 else page) }
                        films = if (reset) result else (films + result).distinctBy { it.id }
                        hasMore = result.isNotEmpty()
                        page = if (reset) 2 else page + 1
                        if (films.isEmpty()) error = "Фильмы не найдены. Попробуйте другое название."
                    } catch (_: Exception) {
                        error = "Не удалось загрузить видеотеку. Проверьте интернет."
                    } finally { loading = false }
                }
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = C_BLUE, background = C_NIGHT, surface = C_PANEL, onSurface = C_TEXT)) {
                Scaffold(
                    containerColor = C_NIGHT,
                    topBar = {
                        TopAppBar(
                            title = { Column {
                                Text("ᚱ  КИНОТЕКА  ᚱ", color = C_TEXT, fontWeight = FontWeight.Black)
                                Text("INTERNET ARCHIVE • PRELINGER", color = C_BLUE, style = MaterialTheme.typography.labelSmall)
                            } },
                            navigationIcon = { IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, "Назад", tint = C_BLUE) } },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = C_PANEL)
                        )
                    }
                ) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp)) {
                        Spacer(Modifier.height(10.dp))
                        Text("Фильмы и исторические киноматериалы", color = C_TEXT, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Открытая коллекция Internet Archive. Доступность и условия использования указаны на странице каждого материала.", color = C_MUTED, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                            singleLine = true, label = { Text("Поиск по названию") },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = C_BLUE) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = C_BLUE, unfocusedBorderColor = C_MUTED, focusedTextColor = C_TEXT, unfocusedTextColor = C_TEXT)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { films = emptyList(); load(true) }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = C_BLUE, contentColor = C_NIGHT)) { Text("НАЙТИ") }
                            OutlinedButton(onClick = { query = ""; films = emptyList(); load(true) }, enabled = !loading) { Text("ВСЯ КОЛЛЕКЦИЯ", color = C_TEXT) }
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = C_BLUE)
                        if (error.isNotBlank()) Text(error, color = Color(0xFFFFB4A9))
                        if (films.isEmpty() && !loading && error.isBlank()) LaunchedEffect(Unit) { load(true) }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 145.dp),
                            state = gridState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(films, key = { it.id }) { film ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { openFilm(film) },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0xFF35658F)),
                                    colors = CardDefaults.cardColors(containerColor = C_PANEL)
                                ) {
                                    Column {
                                        AsyncImage(model = film.poster, contentDescription = film.title, modifier = Modifier.fillMaxWidth().height(185.dp).background(Color(0xFF183D63)), contentScale = ContentScale.Crop)
                                        Column(Modifier.padding(9.dp)) {
                                            Text(film.title, color = C_TEXT, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                            Text(film.year.ifBlank { "Год не указан" }, color = C_MUTED, style = MaterialTheme.typography.labelSmall)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PlayCircle, null, tint = C_BLUE, modifier = Modifier.size(18.dp))
                                                Text("Смотреть", color = C_BLUE, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (loading && films.isNotEmpty()) Text("Загружаем следующие материалы…", color = C_MUTED, style = MaterialTheme.typography.labelSmall)
                        LaunchedEffect(gridState, films.size, loading, hasMore) {
                            snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                                .collect { lastVisible ->
                                    if (hasMore && !loading && films.isNotEmpty() && lastVisible >= films.size - 6) load(false)
                                }
                        }
                    }
                }
            }
        }
    }

    private fun openFilm(film: ArchiveFilm) {
        lifecycleScope.launch {
            try {
                val stream = withContext(Dispatchers.IO) { resolveArchiveVideo(film.id) }
                if (stream == null) {
                    android.widget.Toast.makeText(this@CinemaActivity, "Совместимый видеофайл не найден.", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    startActivity(Intent(this@CinemaActivity, PlayerActivity::class.java).putExtra("url", stream).putExtra("name", film.title))
                }
            } catch (_: Exception) {
                android.widget.Toast.makeText(this@CinemaActivity, "Не удалось открыть видео. Попробуйте позже.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun searchArchiveFilms(search: String, page: Int): List<ArchiveFilm> {
    val q = if (search.isBlank()) "collection:prelinger AND mediatype:movies" else
        "collection:prelinger AND mediatype:movies AND title:\"" + search.replace("\"", "") + "\""
    val encoded = URLEncoder.encode(q, "UTF-8")
    val address = "https://archive.org/advancedsearch.php?q=" + encoded + "&fl[]=identifier&fl[]=title&fl[]=year&rows=30&page=" + page + "&output=json&sort[]=downloads+desc"
    val connection = URL(address).openConnection()
    connection.connectTimeout = 15000
    connection.readTimeout = 25000
    connection.setRequestProperty("User-Agent", "SlovyaninTV/1.4 Android media catalog")
    val root = connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
    val docs = root.optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
    return (0 until docs.length()).mapNotNull { i ->
        val item = docs.optJSONObject(i) ?: return@mapNotNull null
        val id = item.optString("identifier")
        if (id.isBlank()) null else ArchiveFilm(id, item.optString("title", id), item.optString("year"))
    }
}

private fun resolveArchiveVideo(identifier: String): String? {
    val connection = URL("https://archive.org/metadata/" + identifier).openConnection()
    connection.connectTimeout = 15000
    connection.readTimeout = 25000
    connection.setRequestProperty("User-Agent", "SlovyaninTV/1.4 Android media catalog")
    val root = connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
    val files = root.optJSONArray("files") ?: return null
    val choices = mutableListOf<Pair<Long, String>>()
    for (i in 0 until files.length()) {
        val file = files.optJSONObject(i) ?: continue
        val name = file.optString("name")
        val format = file.optString("format")
        if (name.endsWith(".mp4", true) || format.contains("MPEG4", true) || format.contains("h.264", true)) {
            val size = file.optString("size").toLongOrNull() ?: 0L
            if (size > 0L) choices += size to name
        }
    }
    val name = choices.maxByOrNull { it.first }?.second ?: return null
    val safeName = name.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
    return "https://archive.org/download/" + identifier + "/" + safeName
}
