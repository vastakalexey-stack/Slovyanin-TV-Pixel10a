package ru.pixel10a.slovyanintv

data class CatalogSource(
    val title: String,
    val name: String,
    val url: String,
    val description: String
)

/**
 * Sources supplied for this app by the user or assembled from those supplied lists.
 * Public demo catalogs are intentionally not mixed into the user's channel library.
 */
val CatalogSources = listOf(
    CatalogSource(
        title = "Плейлист Egorka",
        name = "Egorka — пользовательский список",
        url = "http://31.57.108.245:8080/Egorka.⅖_1.m3u",
        description = "M3U-источник, ранее присланный пользователем; доступность проверяется при загрузке"
    ),
    CatalogSource(
        title = "Объединённый каталог Slava-67",
        name = "Slava-67 — объединённый список",
        url = "https://raw.githubusercontent.com/vastakalexey-stack/Slovyanin-TV-Pixel10a/main/app/src/main/assets/slava67_combined.m3u",
        description = "Сохранённый объединённый каталог из ранее присланных списков; спортивные каналы отфильтровываются"
    ),
    CatalogSource(
        title = "Free-TV — открытые каналы мира",
        name = "Free-TV — публичный международный каталог",
        url = "https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8",
        description = "Открытые бесплатные каналы из разных стран; часть ссылок YouTube/Twitch может не воспроизводиться напрямую"
    )
)
