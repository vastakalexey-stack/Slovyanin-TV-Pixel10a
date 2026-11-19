package ru.pixel10a.slovyanintv

data class CatalogSource(
    val title: String,
    val name: String,
    val url: String,
    val description: String
)

val CatalogSources = listOf(
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
        title = "Спортивные каналы",
        name = "Каталог IPTV-org — спорт",
        url = "https://iptv-org.github.io/iptv/categories/sports.m3u",
        description = "Спортивные трансляции из открытого каталога"
    ),
    CatalogSource(
        title = "Музыка и радио",
        name = "Открытый плейлист — музыка",
        url = "https://ngrch.github.io/iptv/music.m3u",
        description = "Сторонний публичный плейлист"
    ),
    CatalogSource(
        title = "Детские каналы",
        name = "Открытый плейлист — мультфильмы",
        url = "https://ngrch.github.io/iptv/cartoons.m3u",
        description = "Сторонний публичный плейлист"
    )
)
