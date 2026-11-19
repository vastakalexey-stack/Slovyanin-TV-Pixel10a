package ru.pixel10a.slovyanintv

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CatchupUrlBuilder {
    fun build(
        liveUrl: String,
        mode: String,
        source: String,
        startMillis: Long,
        durationSeconds: Long = 4 * 60 * 60L
    ): String? {
        if (mode.isBlank() || mode.equals("none", true) || mode.equals("disabled", true)) return null
        val utc = startMillis / 1000L
        val local = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date(startMillis))
        val replacements = mapOf(
            "{utc}" to utc.toString(),
            "{timestamp}" to utc.toString(),
            "{start}" to utc.toString(),
            "{utcstr}" to local,
            "{Y}" to SimpleDateFormat("yyyy", Locale.US).format(Date(startMillis)),
            "{m}" to SimpleDateFormat("MM", Locale.US).format(Date(startMillis)),
            "{d}" to SimpleDateFormat("dd", Locale.US).format(Date(startMillis)),
            "{H}" to SimpleDateFormat("HH", Locale.US).format(Date(startMillis)),
            "{M}" to SimpleDateFormat("mm", Locale.US).format(Date(startMillis)),
            "{S}" to SimpleDateFormat("ss", Locale.US).format(Date(startMillis)),
            "{duration}" to durationSeconds.toString(),
            "{lutc}" to (System.currentTimeMillis() / 1000L).toString()
        )
        val encodedSource = replacements.entries.fold(source) { acc, (key, value) ->
            acc.replace(key, value)
        }
        val encodedUrl = replacements.entries.fold(liveUrl) { acc, (key, value) ->
            acc.replace(key, value)
        }
        val resolved = when {
            source.isBlank() -> when (mode.lowercase()) {
                "default", "shift" -> encodedUrl
                else -> null
            }
            source.startsWith("http://", true) || source.startsWith("https://", true) ->
                encodedSource
            mode.equals("append", true) || source.startsWith("?") || source.startsWith("&") ->
                encodedUrl + (if (encodedUrl.contains("?")) "&" else "?") + encodedSource.trimStart('?', '&')
            else -> encodedSource
        } ?: return null
        return resolved.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    }
}
