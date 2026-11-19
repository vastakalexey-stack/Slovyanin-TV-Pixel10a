package ru.pixel10a.slovyanintv

object M3uParser {
    fun parse(text: String, playlistName: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = ""
        var group = "Без категории"
        var logo: String? = null
        var tvgId = ""

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                val attrs = Regex("""([\w-]+)="([^"]*)"""").findAll(line)
                    .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                name = line.substringAfterLast(',', "Канал").trim().ifBlank { "Канал" }
                group = attrs["group-title"]?.ifBlank { "Без категории" } ?: "Без категории"
                logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                tvgId = attrs["tvg-id"]?.trim().orEmpty()
            } else if (line.isNotEmpty() && !line.startsWith("#") &&
                (line.startsWith("http://") || line.startsWith("https://"))) {
                val channel = Channel(
                    name = name.ifBlank { "Канал" },
                    url = line,
                    group = ChannelPolicy.displayGroup(group),
                    logo = logo,
                    playlist = playlistName,
                    tvgId = tvgId
                )
                if (ChannelPolicy.keep(channel)) result += channel
                name = ""
                group = "Без категории"
                logo = null
                tvgId = ""
            }
        }
        return result.distinctBy { it.url }
    }
}
