package ru.pixel10a.slovyanintv

data class CatalogSource(
    val title: String,
    val name: String,
    val url: String,
    val description: String
)

val CatalogSources = listOf(
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
        title = "Музыка и радио",
        name = "Открытый плейлист — музыка",
        url = "https://ngrch.github.io/iptv/music.m3u",
        description = "Сторонний публичный плейлист"
    ),
)
