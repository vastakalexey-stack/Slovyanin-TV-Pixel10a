package ru.pixel10a.slovyanintv

data class StreamSource(
    val url: String,
    val label: String
)

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Без категории",
    val logo: String? = null,
    val playlist: String = "",
    val tvgId: String = "",
    val catchup: String = "",
    val catchupSource: String = "",
    val catchupDays: Int = 0,
    val streams: List<StreamSource> = listOf(StreamSource(url, playlist.ifBlank { "Поток 1" }))
) {
    val hasArchive: Boolean
        get() = catchupSource.isNotBlank() && catchup.isNotBlank() &&
            !catchup.equals("none", true) && !catchup.equals("disabled", true)
}

object ChannelMerger {
    fun merge(channels: List<Channel>): List<Channel> =
        channels.groupBy {
            listOf(it.name.trim().lowercase(), it.group.trim().lowercase(), it.tvgId.trim().lowercase())
                .joinToString("|")
        }.values.map { sameChannel ->
            val first = sameChannel.first()
            val sources = sameChannel.flatMap { channel ->
                channel.streams.ifEmpty { listOf(StreamSource(channel.url, channel.playlist.ifBlank { "Поток" })) }
            }.distinctBy { it.url }
            first.copy(streams = sources)
        }
}
