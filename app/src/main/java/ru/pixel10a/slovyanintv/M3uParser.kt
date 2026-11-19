package ru.pixel10a.slovyanintv
object M3uParser {
    fun parse(text: String, playlistName: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = ""; var group = "Без категории"; var logo: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                val attrs = Regex("""([\w-]+)="([^"]*)"""").findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                name = line.substringAfterLast(',', "Канал").trim().ifBlank { "Канал" }
                group = attrs["group-title"]?.ifBlank { "Без категории" } ?: "Без категории"
                logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
            } else if (line.isNotEmpty() && !line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://"))) {
                result += Channel(name.ifBlank { "Канал" }, line, group, logo, playlistName)
                name = ""; group = "Без категории"; logo = null
            }
        }
        return result
    }
}
