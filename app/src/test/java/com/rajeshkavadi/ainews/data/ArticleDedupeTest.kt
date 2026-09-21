package com.rajeshkavadi.ainews.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Pure-JVM tests for the de-duplication key. These run without Android
 * (no Robolectric needed) because [Article.dedupeKey] is plain Kotlin.
 *
 * Note: the RSS parser itself is NOT unit-tested here because it depends on
 * android.util.Xml, which is unavailable in local JVM tests; it would need an
 * instrumented/Robolectric test.
 */
class ArticleDedupeTest {

    private fun article(title: String) = Article(
        title = title,
        link = "https://example.com/${title.hashCode()}",
        sourceName = "Test",
        summary = null,
        imageUrl = null,
        publishedAtMillis = null
    )

    @Test
    fun `same story with punctuation and case differences collides`() {
        val a = article("OpenAI Launches GPT-5: A New Era!")
        val b = article("openai launches gpt 5 a new era")
        assertEquals(a.dedupeKey, b.dedupeKey)
    }

    @Test
    fun `different stories do not collide`() {
        val a = article("Google releases Gemini 3")
        val b = article("Meta open-sources Llama 4")
        assertNotEquals(a.dedupeKey, b.dedupeKey)
    }

    @Test
    fun `extra whitespace is normalized away`() {
        val a = article("Anthropic   ships    Claude")
        val b = article("Anthropic ships Claude")
        assertEquals(a.dedupeKey, b.dedupeKey)
    }
}
