package com.rajeshkavadi.ainews.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * A minimal, dependency-free feed parser built on Android's [XmlPullParser].
 * Handles both RSS 2.0 (`<item>`) and Atom (`<entry>`) in a single pass, and is
 * tolerant of missing fields — a malformed item is skipped, not fatal.
 */
object RssParser {

    /**
     * Parses [input] into normalized [Article]s tagged with [sourceName].
     * Never throws for per-item problems; a parse-level failure propagates so the
     * repository can mark this one source as failed.
     */
    fun parse(input: InputStream, sourceName: String): List<Article> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        val articles = mutableListOf<Article>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                val tag = parser.name?.lowercase()
                if (tag == "item" || tag == "entry") {
                    parseEntry(parser, tag, sourceName)?.let { articles.add(it) }
                }
            }
            event = parser.next()
        }
        return articles
    }

    private fun parseEntry(parser: XmlPullParser, entryTag: String, sourceName: String): Article? {
        var title: String? = null
        var link: String? = null
        var summary: String? = null
        var imageUrl: String? = null
        var dateText: String? = null

        while (!(parser.eventType == XmlPullParser.END_TAG &&
                    parser.name?.lowercase() == entryTag)
        ) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                // Namespaces are off, so tags keep prefixes (media:content,
                // content:encoded). Strip the prefix to a local name.
                val local = parser.name?.substringAfterLast(':')?.lowercase()
                when (local) {
                    "title" -> title = readText(parser)
                    "link" -> {
                        // RSS: <link>url</link>. Atom: <link href="url" rel="alternate"/>.
                        val href = parser.getAttributeValue(null, "href")
                        val rel = parser.getAttributeValue(null, "rel")
                        if (href != null) {
                            if (link == null || rel == null || rel == "alternate") link = href
                            skipToEndTag(parser)
                        } else {
                            val text = readText(parser)
                            if (!text.isNullOrBlank()) link = text
                        }
                    }
                    "content" -> {
                        // Ambiguous: media:content carries a url attribute (image),
                        // Atom <content> carries HTML text. Attribute wins if present.
                        val url = parser.getAttributeValue(null, "url")
                        if (url != null) {
                            val type = parser.getAttributeValue(null, "type")
                            if (imageUrl == null && (type == null || type.startsWith("image"))) {
                                imageUrl = url
                            }
                            skipToEndTag(parser)
                        } else {
                            val text = readText(parser)
                            if (summary == null && !text.isNullOrBlank()) summary = stripHtml(text)
                            if (imageUrl == null && text != null) imageUrl = firstImgSrc(text)
                        }
                    }
                    "description", "summary", "encoded" -> {
                        val text = readText(parser)
                        if (summary == null && !text.isNullOrBlank()) summary = stripHtml(text)
                        if (imageUrl == null && text != null) imageUrl = firstImgSrc(text)
                    }
                    "pubdate", "published", "updated", "date" -> {
                        val text = readText(parser)
                        if (dateText == null && !text.isNullOrBlank()) dateText = text
                    }
                    "thumbnail" -> {
                        // media:thumbnail carries the image as a url attribute.
                        val url = parser.getAttributeValue(null, "url")
                        if (imageUrl == null && !url.isNullOrBlank()) imageUrl = url
                    }
                    "enclosure" -> {
                        val type = parser.getAttributeValue(null, "type")
                        val url = parser.getAttributeValue(null, "url")
                        if (imageUrl == null && !url.isNullOrBlank() &&
                            (type == null || type.startsWith("image"))
                        ) imageUrl = url
                    }
                }
            }
            if (parser.next() == XmlPullParser.END_DOCUMENT) break
        }

        val cleanTitle = title?.trim().orEmpty()
        val cleanLink = link?.trim().orEmpty()
        if (cleanTitle.isBlank() || cleanLink.isBlank()) return null

        return Article(
            title = cleanTitle,
            link = cleanLink,
            sourceName = sourceName,
            summary = summary?.take(280),
            imageUrl = imageUrl,
            publishedAtMillis = parseDate(dateText)
        )
    }

    /** Reads the text content of the current element, advancing past its end tag. */
    private fun readText(parser: XmlPullParser): String? {
        var result: String? = null
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    /** Fast-forwards to the END_TAG of a self-contained/attribute-only element. */
    private fun skipToEndTag(parser: XmlPullParser) {
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }

    private fun stripHtml(html: String): String =
        html.replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&#8217;", "’")
            .replace("&#8216;", "‘")
            .replace("&#8220;", "“")
            .replace("&#8221;", "”")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun firstImgSrc(html: String): String? =
        Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)

    // RSS uses RFC-822; Atom uses RFC-3339. Try the common shapes, in order.
    private val dateFormats: List<SimpleDateFormat> = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssZ"
    ).map { SimpleDateFormat(it, Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    private fun parseDate(text: String?): Long? {
        val t = text?.trim() ?: return null
        for (fmt in dateFormats) {
            try {
                return fmt.parse(t)?.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }
}
