package ru.pixel10a.slovyanintv

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Без категории",
    val logo: String? = null,
    val playlist: String = "",
    val tvgId: String = "",
    val catchup: String = "",
    val catchupSource: String = "",
    val catchupDays: Int = 0
) {
    val hasArchive: Boolean
        get() = catchupSource.isNotBlank() || catchup.equals("default", true) ||
            catchup.equals("append", true) || catchup.equals("shift", true)
}
