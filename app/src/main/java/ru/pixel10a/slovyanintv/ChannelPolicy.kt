package ru.pixel10a.slovyanintv

/**
 * Removes sports channels and normalizes playlist labels for a Russian-language UI.
 */
object ChannelPolicy {
    private val sportsTerms = listOf(
        "спорт", "спортив", "футбол", "football", "soccer",
        "хоккей", "hockey", "баскетбол", "basketball", "теннис", "tennis",
        "формула 1", "formula 1", "f1", "бокс", "boxing", "mma", "ufc",
        "wrestling", "борьб", "регби", "rugby", "volleyball", "волейбол",
        "биатлон", "biathlon", "гандбол", "handball", "крикет", "cricket",
        "dazn", "match tv", "матч тв", "матч!", "eurosport", "setanta sports",
        "sky sports", "espn", "fox sports", "sport 1", "sport 2", "sport tv",
        "arena sport", "bein sports", "eleven sports", "канал футбол"
    )

    private val channelNames = mapOf(
        "1TV" to "Первый канал", "CHANNEL ONE RUSSIA" to "Первый канал",
        "ROSSIYA 1" to "Россия 1", "RUSSIA 1" to "Россия 1",
        "ROSSIYA 24" to "Россия 24", "RUSSIA 24" to "Россия 24",
        "NTV" to "НТВ", "TVC" to "ТВ Центр", "TV CENTER" to "ТВ Центр",
        "STS" to "СТС", "CTC" to "СТС", "REN TV" to "РЕН ТВ",
        "REN-TV" to "РЕН ТВ", "TV3" to "ТВ-3", "TV 3" to "ТВ-3",
        "DOMASHNIY" to "Домашний", "DOMASHNY" to "Домашний",
        "ZVEZDA" to "Звезда", "MIR" to "Мир", "OTR" to "ОТР",
        "KULTURA" to "Россия Культура", "RUSSIA K" to "Россия К",
        "SPAS" to "Спас", "STS LOVE" to "СТС Love", "CHE" to "Че",
        "FRIDAY" to "Пятница!", "PYATNITSA" to "Пятница!",
        "TV 1000" to "ТВ-1000", "KINOPREMIERA" to "Кинопремьера",
        "KINOSERIA" to "Киносерия", "MIR 24" to "Мир 24",
        "BBC WORLD NEWS" to "Би-би-си Новости", "BBC NEWS" to "Би-би-си Новости",
        "EURONEWS" to "Евроньюс", "CNN" to "Си-эн-эн",
        "DISCOVERY CHANNEL" to "Дискавери", "NATIONAL GEOGRAPHIC" to "National Geographic",
        "NAT GEO" to "National Geographic", "ANIMAL PLANET" to "Энимал Планет",
        "CARTOON NETWORK" to "Картун Нетворк", "DISNEY CHANNEL" to "Дисней",
        "NICK JR" to "Никелодеон Джуниор", "NICKELODEON" to "Никелодеон",
        "RUTUBE" to "Рутуб", "RT" to "РТ", "RT DOCUMENTARY" to "РТ Док",
        "TNT" to "ТНТ", "TNT4" to "ТНТ4", "2X2" to "2х2"
    )

    private val transliteration = mapOf(
        'a' to "а", 'b' to "б", 'c' to "к", 'd' to "д", 'e' to "е",
        'f' to "ф", 'g' to "г", 'h' to "х", 'i' to "и", 'j' to "дж",
        'k' to "к", 'l' to "л", 'm' to "м", 'n' to "н", 'o' to "о",
        'p' to "п", 'q' to "к", 'r' to "р", 's' to "с", 't' to "т",
        'u' to "у", 'v' to "в", 'w' to "в", 'x' to "кс", 'y' to "й",
        'z' to "з"
    )

    fun isSports(name: String, group: String): Boolean {
        val value = "${name.lowercase()} ${group.lowercase()}"
        return Regex("""(?i)(?<![a-z])sports?(?![a-z])""").containsMatchIn(value) ||
            sportsTerms.any { value.contains(it) }
    }

    fun keep(channel: Channel): Boolean = !isSports(channel.name, channel.group)

    /** Translate known channel brands and transliterate other Latin-only names. */
    fun displayChannelName(raw: String): String {
        val value = raw.trim()
        if (value.isBlank()) return "Канал"
        channelNames[value.uppercase()]?.let { return it }
        if (value.any { it in 'А'..'я' || it == 'Ё' || it == 'ё' }) return value
        val hasLatin = value.any { it in 'A'..'Z' || it in 'a'..'z' }
        if (!hasLatin) return value
        return value.lowercase().map { ch ->
            when {
                ch in transliteration -> transliteration[ch]!!
                ch.isDigit() || ch.isWhitespace() || ch in ".,:!?()[]+-_/'&" -> ch.toString()
                else -> ch.toString()
            }
        }.joinToString("").replaceFirstChar { it.titlecase() }
    }

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
            else -> displayChannelName(value)
        }
    }
}
