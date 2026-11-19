package ru.pixel10a.slovyanintv

object M3uParser {
    fun parse(text: String, playlistName: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = ""
        var group = "Без категории"
        var logo: String? = null
        var tvgId = ""
        var catchup = ""
        var catchupSource = ""
        var catchupDays = 0

        text.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.startsWith("#EXTINF", true)) {
                val attrs = Regex("""([A-Za-z0-9_-]+)="([^"]*)""").findAll(line)
                    .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                name = line.substringAfterLast(',', "Канал").trim().ifBlank { "Канал" }
                group = attrs["group-title"]?.ifBlank { "Без категории" } ?: "Без категории"
                logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                tvgId = attrs["tvg-id"]?.trim().orEmpty()
                catchup = attrs["catchup"]?.trim().orEmpty()
                catchupSource = attrs["catchup-source"]?.trim().orEmpty()
                catchupDays = attrs["catchup-days"]?.toIntOrNull()?.coerceIn(0, 30) ?: 0
            } else if (line.isNotEmpty() && !line.startsWith("#") &&
                (line.startsWith("http://") || line.startsWith("https://"))) {
                val channel = Channel(
                    name = name.ifBlank { "Канал" },
                    url = line,
                    group = ChannelPolicy.displayGroup(group),
                    logo = logo,
                    playlist = playlistName,
                    tvgId = tvgId,
                    catchup = catchup,
                    catchupSource = catchupSource,
                    catchupDays = catchupDays
                )
                if (ChannelPolicy.keep(channel)) result += channel
                name = ""
                group = "Без категории"
                logo = null
                tvgId = ""
                catchup = ""
                catchupSource = ""
                catchupDays = 0
            }
        }
        return result.distinctBy { it.url }
    }
}
