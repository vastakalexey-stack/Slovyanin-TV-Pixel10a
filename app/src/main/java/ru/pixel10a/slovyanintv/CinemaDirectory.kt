package ru.pixel10a.slovyanintv

data class CinemaDirectoryEntry(
    val family: String,
    val name: String
)

/**
 * Channel names transcribed from the user's Kinozal screenshots.
 * Entries without a verified authorized stream are directory-only and are never
 * assigned guessed URLs or subscriber credentials.
 */
object CinemaDirectory {
    val entries = listOf(
        "BCU" to listOf(
            "BCU Action", "BCU Catastrophe", "BCU Cinema", "BCU Cinema+", "BCU Comedy HDR",
            "BCU Cosmo", "BCU FilMystic", "BCU History", "BCU Kinorating", "BCU Marvel",
            "BCU Media", "BCU Premiere", "BCU Premiere 1", "BCU Premiere 2", "BCU Premiere 3",
            "BCU Russian", "BCU Stars", "BCU Survival", "BCU TruMotion", "BCU VHS",
            "BCU Сваты", "BCU СССР"
        ),
        "BOX" to listOf(
            "BOX Apocalypse", "BOX AutoTrend", "BOX Gangster", "BOX Ghost", "BOX Gurman",
            "BOX Hybrid", "BOX Mayday", "BOX Memory", "BOX Serial", "BOX Spy",
            "BOX Travel Premiere", "BOX Zombie"
        ),
        "CineMan" to listOf(
            "CineMan", "CineMan Action", "CineMan Air Crash", "CineMan Marvel",
            "CineMan MiniSeries", "CineMan Premium", "CineMan Thriller", "CineMan VHS",
            "CineMan Комедийные сериалы", "CineMan Комедия", "CineMan Криминальные Сериалы",
            "CineMan Лесник", "CineMan Мелодрама", "CineMan ПЁС + Лихач", "CineMan РуКино",
            "CineMan Сваты", "CineMan Скорая помощь"
        ),
        "Citrus" to listOf("Citrus History", "Citrus Inquest", "Citrus Marvel"),
        "Eye" to listOf(
            "Eye Criminal", "Eye Cyber", "Eye Drive", "Eye Family", "Eye Frozen",
            "Eye History", "Eye Oscar", "Eye Western"
        ),
        "Fresh" to listOf(
            "Fresh Cinema", "Fresh Comedy", "Fresh Family", "Fresh Fantastic", "Fresh Horror",
            "Fresh Premiere", "Fresh Rating", "Fresh Soviet", "Fresh Thriller", "Fresh VHS"
        ),
        "Liberty" to listOf(
            "Liberty DC", "Liberty Боевики", "Liberty Занавес", "Liberty Кино УКВ",
            "Liberty Комедии", "Liberty Короткометражки", "Liberty Марвел", "Liberty Мелодрамы",
            "Liberty Мим", "Liberty Сваты", "Liberty Сериалы", "Liberty Фантастика", "Liberty Эротика"
        ),
        "Magic" to listOf(
            "Magic Comedy", "Magic Family", "Magic Galaxy", "Magic Horror", "Magic Karate",
            "Magic Love", "Magic Thriller", "Magic VHS"
        ),
        "VeleS" to listOf(
            "VeleS 60fps", "VeleS Movie Hits", "VeleS Premiere", "VeleS Киноплёнка",
            "VeleS Киносерия", "VeleS СССР", "VeleS страшное"
        ),
        "VF" to listOf(
            "VF Art house", "VF Classic", "VF Comedy", "VF Comics", "VF Fantastic",
            "VF Fantasy", "VF HBO", "VF Mystic 2", "VF Premiere", "VF Series",
            "VF The X-Files", "VF VHS MIX", "VF Без цензуры", "VF Беларусьфильм",
            "VF Бесстыжие", "VF Боевик", "VF Воронины", "VF Друзья", "VF Карточный домик",
            "VF Катастрофы", "VF Клиника", "VF Криминал", "VF Луи де Фюнес",
            "VF Наша победа", "VF Наша Раша", "VF Однажды в России", "VF Реальные пацаны",
            "VF Рижская киностудия", "VF СашаТаня", "VF Сваты", "VF Секс в большом городе",
            "VF Сериал", "VF Солдаты", "VF Ужасы 2", "VF Ужасы VHS", "VF Универ",
            "VF Универ новая общага", "VF Фильмы СССР", "VF Ходячие мертвецы",
            "VF Чернобыль", "VF Ёлки"
        ),
        "Yosso TV" to listOf(
            "Yosso TV 4k", "Yosso TV Adrenaline", "Yosso TV Adventure", "Yosso TV Best",
            "Yosso TV Best 60fps", "Yosso TV Disney", "Yosso TV Grand", "Yosso TV Netflix",
            "Yosso TV New Кино", "Yosso TV Oblivion", "Yosso TV Thriller", "Yosso TV VHS",
            "Yosso TV Забавное", "Yosso TV Ковбойское", "Yosso TV Мелодрама",
            "Yosso TV Русские Фильмы", "Yosso TV Советские Фильмы",
            "Yosso TV Советские Фильмы 4K", "Yosso TV Трагичное"
        ),
        "Другие" to listOf("TOP Cinema", "A&E HD US", "American Heroes Channel HD US", "AXS TV HD US", "BabyFirst TV HD US")
    ).flatMap { (family, names) -> names.map { CinemaDirectoryEntry(family, it) } }
        .distinctBy { it.name.lowercase() }

    fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    fun isKnown(channelName: String): Boolean {
        val normalized = normalize(channelName)
        return entries.any { normalize(it.name) == normalized }
    }
}
