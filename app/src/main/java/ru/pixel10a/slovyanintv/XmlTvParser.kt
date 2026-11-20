package ru.pixel10a.slovyanintv

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.zip.GZIPInputStream

data class TvProgram(
    val start: Long,
    val stop: Long,
    val title: String,
    val description: String
)

object XmlTvParser {
    fun load(address: String, tvgId: String, channelName: String): List<TvProgram> {
        require(address.startsWith("https://") || address.startsWith("http://")) {
            "XMLTV URL must use HTTP or HTTPS"
        }
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.setRequestProperty("Accept-Encoding", "gzip")
        try {
            connection.connect()
            val raw = connection.inputStream
            val stream = if (
                address.substringBefore('?').endsWith(".gz", true) ||
                connection.contentEncoding.equals("gzip", true)
            ) GZIPInputStream(raw) else raw

            val parser = Xml.newPullParser()
            parser.setInput(stream, "UTF-8")
            val results = mutableListOf<TvProgram>()
            val wantedId = tvgId.trim()
            val wantedName = normalize(channelName)
            var event = parser.eventType
            var matching = false
            var start = 0L
            var stop = 0L
            var title = ""
            var description = ""
            val now = System.currentTimeMillis()
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "programme" -> {
                            val id = parser.getAttributeValue(null, "channel").orEmpty()
                            matching = if (wantedId.isNotBlank()) id.equals(wantedId, true)
                                else normalize(id) == wantedName || normalize(id).contains(wantedName)
                            start = parseXmlDate(parser.getAttributeValue(null, "start").orEmpty())
                            stop = parseXmlDate(parser.getAttributeValue(null, "stop").orEmpty())
                            title = ""
                            description = ""
                        }
                        "title" -> if (matching) title = parser.nextText().trim()
                        "desc" -> if (matching) description = parser.nextText().trim()
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "programme" && matching) {
                    if (start > 0 && stop > start && stop > now - 3600000 && title.isNotBlank()) {
                        results += TvProgram(start, stop, title, description)
                    }
                    matching = false
                }
                event = parser.next()
            }
            stream.close()
            return results.sortedBy { it.start }.take(100)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseXmlDate(value: String): Long {
        val formats = listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss")
        for (pattern in formats) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                format.isLenient = false
                if (pattern.endsWith("ss")) format.timeZone = TimeZone.getDefault()
                return format.parse(value)?.time ?: 0L
            } catch (_: Exception) { }
        }
        return 0L
    }

    private fun normalize(value: String): String =
        value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")
    fun loadFromSources(addresses: List<String>, tvgId: String, channelName: String): List<TvProgram> {
        var lastError: Exception? = null
        addresses.filter { it.isNotBlank() }.distinct().forEach { address ->
            try {
                val result = load(address, tvgId, channelName)
                if (result.isNotEmpty()) return result
            } catch (error: Exception) {
                lastError = error
            }
        }
        if (lastError != null) throw lastError as Exception
        return emptyList()
    }

}
