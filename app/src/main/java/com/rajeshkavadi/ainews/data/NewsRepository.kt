package com.rajeshkavadi.ainews.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Collections
import java.util.concurrent.TimeUnit

/** Outcome of a headline load: the ranked list plus which sources failed. */
data class HeadlinesResult(
    val articles: List<Article>,
    val failedSources: List<String>
)

/**
 * Fetches feeds in parallel and merges them into a single ranked list, per tab.
 *
 * Design decisions worth defending:
 *  - One slow or dead feed must not block or fail the whole screen, so each fetch
 *    is isolated and failures are collected, not thrown.
 *  - "Top 15" is recency-ranked with a per-source cap, NOT an importance ranking.
 *  - For the Start-ups tab the list is *composed*: ~5 space items (space feeds,
 *    company-named items prioritized) + the rest from global startup feeds. The 5
 *    is a target with recency backfill, not a hard guarantee that all 5 are pure
 *    "startups" — no on-device signal can promise that.
 */
class NewsRepository(
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun loadTopHeadlines(category: Category, limit: Int = 15): HeadlinesResult =
        coroutineScope {
            val failed = Collections.synchronizedList(mutableListOf<String>())
            val articles = when (category) {
                Category.AI ->
                    rank(fetchAll(NewsSources.AI_SOURCES, failed), limit)

                Category.STARTUPS -> {
                    val spaceDef = async { fetchAll(NewsSources.STARTUP_SPACE_SOURCES, failed) }
                    val generalDef = async { fetchAll(NewsSources.STARTUP_GENERAL_SOURCES, failed) }
                    composeStartups(
                        space = spaceDef.await(),
                        general = generalDef.await(),
                        limit = limit,
                        spaceQuota = 5
                    )
                }
            }
            HeadlinesResult(articles = articles, failedSources = failed.toList())
        }

    /** Fetches every source concurrently; per-source failures are recorded, not thrown. */
    private suspend fun fetchAll(
        sources: List<NewsSource>,
        failed: MutableList<String>
    ): List<Article> = coroutineScope {
        sources.map { source ->
            async(Dispatchers.IO) {
                try {
                    fetchAndParse(source)
                } catch (e: Exception) {
                    failed.add(source.name)
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    private fun fetchAndParse(source: NewsSource): List<Article> {
        val request = Request.Builder()
            .url(source.feedUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} for ${source.feedUrl}")
            }
            val body = response.body ?: throw IllegalStateException("Empty body for ${source.feedUrl}")
            val raw = body.byteStream().use { RssParser.parse(it, source.name) }
            val keywords = source.filterKeywords
            return if (keywords == null) raw else raw.filter { matchesAny(it, keywords) }
        }
    }

    private fun matchesAny(article: Article, keywords: List<String>): Boolean {
        val haystack = " ${article.title.lowercase()} ${article.summary?.lowercase().orEmpty()} "
        return keywords.any { haystack.contains(it) }
    }

    // --- AI ranking: dedupe -> recency -> per-source cap -> backfill --------------
    private fun rank(articles: List<Article>, limit: Int): List<Article> {
        val deduped = dedupeSort(articles)
        val maxPerSource = maxOf(2, limit / 3)
        return capPerSource(deduped, limit, maxPerSource)
    }

    // --- Start-ups composition ---------------------------------------------------
    private fun composeStartups(
        space: List<Article>,
        general: List<Article>,
        limit: Int,
        spaceQuota: Int
    ): List<Article> {
        // Prioritize space items that name an actual company, then by recency.
        val spaceRanked = dedupeSort(space).sortedWith(
            compareByDescending<Article> { matchesAny(it, NewsSources.SPACE_COMPANY_KEYWORDS) }
                .thenByDescending { it.publishedAtMillis ?: Long.MIN_VALUE }
        )
        val spacePick = spaceRanked.take(spaceQuota)
        val takenKeys = spacePick.mapTo(mutableSetOf()) { it.dedupeKey }

        // Fill the remaining slots from global startup feeds (excluding dupes),
        // capped per source for diversity.
        val generalRanked = dedupeSort(general).filter { it.dedupeKey !in takenKeys }
        val generalPick = capPerSource(generalRanked, limit - spacePick.size, maxPerSource = 3)

        var combined = spacePick + generalPick

        // Backfill if either pool came up short (few feeds responded).
        if (combined.size < limit) {
            val keys = combined.mapTo(mutableSetOf()) { it.dedupeKey }
            val leftovers = dedupeSort(space + general).filter { it.dedupeKey !in keys }
            combined = combined + leftovers.take(limit - combined.size)
        }

        // Display newest-first while preserving the composed membership.
        return combined
            .sortedByDescending { it.publishedAtMillis ?: Long.MIN_VALUE }
            .take(limit)
    }

    // --- Shared helpers ----------------------------------------------------------
    private fun dedupeSort(articles: List<Article>): List<Article> =
        articles
            .filter { it.dedupeKey.isNotBlank() }
            .associateBy { it.dedupeKey }   // last write wins for dupes
            .values
            .sortedByDescending { it.publishedAtMillis ?: Long.MIN_VALUE }

    private fun capPerSource(
        sorted: List<Article>,
        limit: Int,
        maxPerSource: Int
    ): List<Article> {
        if (limit <= 0) return emptyList()
        val counts = mutableMapOf<String, Int>()
        val out = mutableListOf<Article>()
        for (a in sorted) {
            val c = counts.getOrDefault(a.sourceName, 0)
            if (c < maxPerSource) {
                out.add(a)
                counts[a.sourceName] = c + 1
            }
            if (out.size == limit) break
        }
        if (out.size < limit) {
            for (a in sorted) {
                if (out.size == limit) break
                if (a !in out) out.add(a)
            }
        }
        return out
    }

    companion object {
        private const val USER_AGENT =
            "AINews/1.0 (Android; +https://github.com/rajeshkavadi/AINews)"

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
