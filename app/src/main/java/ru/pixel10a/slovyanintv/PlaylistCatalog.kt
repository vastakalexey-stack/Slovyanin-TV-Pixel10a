package ru.pixel10a.slovyanintv

data class CatalogSource(
    val title: String,
    val name: String,
    val url: String,
    val description: String
)

val CatalogSources = listOf(
    CatalogSource(
        title = "Открытые киноканалы",
        name = "IPTV-org — Movies",
        url = "https://iptv-org.github.io/iptv/categories/movies.m3u",
        description = "Публичные открытые телеканалы категории Movies; это эфирные каналы, не каталог фильмов по запросу"
    ),
    CatalogSource(
        title = "Плейлист Egorka",
        name = "Egorka — пользовательский поток",
        url = "http://31.57.108.245:8080/Egorka.⅖_1.m3u",
        description = "Ранее присланный пользователем M3U-источник; доступность проверяется при загрузке"
    ),
    CatalogSource(
        title = "Все русскоязычные каналы",
        name = "Каталог IPTV-org — русский язык",
        url = "https://iptv-org.github.io/iptv/languages/rus.m3u",
        description = "Каналы на русском языке из разных стран"
    ),
    CatalogSource(
        title = "Каналы России",
        name = "Каталог IPTV-org — Россия",
        url = "https://iptv-org.github.io/iptv/countries/ru.m3u",
        description = "Список каналов, отнесённых к России"
    ),
    CatalogSource(
        title = "Список Slava-67 — полный",
        name = "Slava-67 — avto-full",
        url = "https://raw.githubusercontent.com/slava-67/TV/refs/heads/IPTV/avto-full.m3u",
        description = "Полный пользовательский список; сохраняем каналы и потоки с VPN, спортивные каналы исключаются"
    ),
    CatalogSource(
        title = "Список Slava-67 — IPTV TVA",
        name = "Slava-67 — avto-iptv-tva",
        url = "https://raw.githubusercontent.com/slava-67/TV/refs/heads/IPTV/avto-iptv-tva.m3u",
        description = "Каналы и кинозалы, включая потоки с VPN; спортивные каналы исключаются"
    ),
    CatalogSource(
        title = "Музыка и радио",
        name = "Открытый плейлист — музыка",
        url = "https://ngrch.github.io/iptv/music.m3u",
        description = "Сторонний публичный плейлист"
    ),
)
