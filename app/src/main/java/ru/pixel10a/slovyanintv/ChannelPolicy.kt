package ru.pixel10a.slovyanintv

/**
 * Central policy for hiding sports channels and presenting category names in Russian.
 * Filtering is applied to imported playlists and restored libraries alike.
 */
object ChannelPolicy {
    private val sportsTerms = listOf(
        "sport", "sports", "спорт", "спортив", "футбол", "football", "soccer",
        "хоккей", "hockey", "баскетбол", "basketball", "теннис", "tennis",
        "формула 1", "formula 1", "f1", "бокс", "boxing", "mma", "ufc",
        "wrestling", "борьб", "регби", "rugby", "volleyball", "волейбол",
        "биатлон", "biathlon", "гандбол", "handball", "крикет", "cricket",
        "dazn", "match tv", "матч тв", "матч!", "eurosport", "setanta sports",
        "sky sports", "espn", "fox sports", "sport 1", "sport 2", "sport tv",
        "arena sport", "bein sports", "eleven sports", "канал футбол"
    )

    fun isSports(name: String, group: String): Boolean {
        val value = "${name.lowercase()} ${group.lowercase()}"
        return sportsTerms.any { value.contains(it) }
    }

    fun keep(channel: Channel): Boolean = !isSports(channel.name, channel.group)

    fun displayGroup(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return "Без категории"
        val key = value.lowercase().replace('_', ' ').replace('-', ' ').trim()
        return when {
            key in setOf("без категории", "uncategorized", "other", "others", "undefined", "none") -> "Без категории"
            key.contains("news") || key.contains("новост") -> "Новости"
            key.contains("movie") || key.contains("film") || key.contains("кино") || key.contains("фильм") -> "Фильмы"
            key.contains("series") || key.contains("сериал") -> "Сериалы"
            key.contains("child") || key.contains("kids") || key.contains("cartoon") || key.contains("дет") || key.contains("мульт") -> "Детские"
            key.contains("music") || key.contains("музык") -> "Музыка"
            key.contains("document") || key.contains("познав") -> "Познавательные"
            key.contains("entertain") || key.contains("развлек") || key.contains("show") -> "Развлекательные"
            key.contains("general") || key.contains("общ") -> "Общие"
            key.contains("relig") || key.contains("религ") -> "Религиозные"
            key.contains("local") || key.contains("регион") -> "Региональные"
            key.contains("classic") -> "Классика"
            key.contains("auto") || key.contains("авто") -> "Автомобили"
            key.contains("culture") || key.contains("культур") -> "Культура"
            key.contains("education") || key.contains("образован") -> "Образование"
            else -> value.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
