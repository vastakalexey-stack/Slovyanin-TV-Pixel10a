package ru.pixel10a.slovyanintv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {
    private val imported = mutableStateOf<List<Channel>>(emptyList())
    private val favorites = mutableStateListOf<String>()
    private val openFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { loadFile(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showDialog by remember { mutableStateOf(false) }
            var url by remember { mutableStateOf("") }
            var loading by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf("") }
            var selectedGroup by remember { mutableStateOf("Все") }
            val channels = imported.value
            val groups = listOf("Все", "Избранное") + channels.map { it.group }.distinct().sorted()
            val shown = channels.filter { (selectedGroup == "Все" || selectedGroup == "Избранное" && it.url in favorites || it.group == selectedGroup) && (selectedGroup != "Избранное" || it.url in favorites) }
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFC49A55), secondary = Color(0xFF9B7844), background = Color(0xFF11100E), surface = Color(0xFF1D1A16))) {
                Scaffold(containerColor = Color(0xFF11100E), topBar = {
                    TopAppBar(title = { Column { Text("СЛОВЯНИН ТВ", fontWeight = FontWeight.Bold); Text("Телевидение в твоём телефоне", style = MaterialTheme.typography.labelSmall, color = Color.Gray) } },
                        actions = { IconButton(onClick = { showDialog = true }) { Icon(Icons.Default.AddLink, "Добавить плейлист") } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF191612), titleContentColor = Color(0xFFE7D7B8)))
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Каналы", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE7D7B8), modifier = Modifier.weight(1f))
                            TextButton(onClick = { selectedGroup = if (selectedGroup == "Избранное") "Все" else "Избранное" }) { Icon(Icons.Default.Favorite, null); Text("Избранное") }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = { openFile.launch(arrayOf("*/*")) }, label = { Text("Импорт M3U") }, leadingIcon = { Icon(Icons.Default.FileOpen, null) })
                            Spacer(Modifier.width(8.dp))
                            AssistChip(onClick = { showDialog = true }, label = { Text("Добавить по ссылке") }, leadingIcon = { Icon(Icons.Default.Link, null) })
                        }
                        Spacer(Modifier.height(12.dp))
                        if (channels.isNotEmpty()) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(groups) { group -> FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group }, label = { Text(group) }) }
                        }
                        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                        if (shown.isEmpty()) {
                            Spacer(Modifier.height(48.dp))
                            Icon(Icons.Default.Tv, null, tint = Color(0xFFC49A55), modifier = Modifier.size(54.dp).align(Alignment.CenterHorizontally))
                            Text(if (channels.isEmpty()) "Пока нет каналов" else "В этой категории пока пусто", color = Color(0xFFE7D7B8), style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp))
                            Text("Добавь собственный плейлист M3U/M3U8 или импортируй файл с телефона.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        } else LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(shown, key = { it.url }) { channel ->
                                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF211E19))) {
                                    Row(Modifier.fillMaxWidth().clickable { startActivity(Intent(this@MainActivity, PlayerActivity::class.java).putExtra("url", channel.url).putExtra("name", channel.name)) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayCircle, null, tint = Color(0xFFC49A55), modifier = Modifier.size(32.dp)); Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) { Text(channel.name, color = Color(0xFFE7D7B8), fontWeight = FontWeight.SemiBold); Text(channel.group, color = Color.Gray, style = MaterialTheme.typography.labelSmall) }
                                        IconButton(onClick = { if (channel.url in favorites) favorites.remove(channel.url) else favorites.add(channel.url) }) { Icon(Icons.Default.Favorite, null, tint = if (channel.url in favorites) Color(0xFFC49A55) else Color.Gray) }
                                    }
                                }
                            }
                        }
                    }
                }
                if (showDialog) AlertDialog(onDismissRequest = { showDialog = false },
                    title = { Text("Добавить плейлист") },
                    text = { Column { Text("Вставь ссылку на свой M3U или M3U8-плейлист."); OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("https://…") }, singleLine = true) } },
                    confirmButton = { TextButton(enabled = url.startsWith("https://") || url.startsWith("http://"), onClick = {
                        val link = url.trim(); showDialog = false; loading = true; error = ""
                        scope.launch {
                            try {
                                val body = withContext(Dispatchers.IO) { URL(link).openStream().bufferedReader().use { it.readText() } }
                                val parsed = M3uParser.parse(body, link.substringAfterLast('/').ifBlank { "Онлайн-плейлист" })
                                if (parsed.isEmpty()) error = "В плейлисте не найдено каналов."
                                else imported.value = (channels + parsed).distinctBy { it.url }
                            } catch (_: Exception) { error = "Не удалось загрузить плейлист. Проверь ссылку и подключение." }
                            loading = false; url = ""
                        }
                    }) { Text("Загрузить") } },
                    dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Отмена") } })
            }
        }
    }
    private fun loadFile(uri: Uri) {
        try {
            val body = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
            imported.value = (imported.value + M3uParser.parse(body, uri.lastPathSegment ?: "Файл")).distinctBy { it.url }
        } catch (_: Exception) { }
    }
}
