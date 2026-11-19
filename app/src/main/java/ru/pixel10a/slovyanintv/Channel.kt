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
        get() = catchupSource.isNotBlank() && catchup.isNotBlank() &&
            !catchup.equals("none", true) && !catchup.equals("disabled", true)
}
