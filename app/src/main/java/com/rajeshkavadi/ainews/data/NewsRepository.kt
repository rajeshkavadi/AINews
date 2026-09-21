package com.rajeshkavadi.ainews.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Outcome of a headline load: the ranked list plus which sources failed. */
data class HeadlinesResult(
    val articles: List<Article>,
    val failedSources: List<String>
)

/**
 * Fetches every configured feed in parallel, then merges the results into a
 * single ranked list.
 *
 * Design decisions worth defending:
 *  - One slow or dead feed must not block or fail the whole screen, so each fetch
 *    is isolated and failures are collected, not thrown.
 *  - "Top 15" is explicitly recency-ranked with a per-source cap, NOT an
 *    importance ranking. On-device we have no signal for global importance; the
 *    cap just stops one prolific feed from crowding out the rest.
 */
class NewsRepository(
    private val sources: List<NewsSource> = NewsSources.ALL,
    private val client: OkHttpClient = defaultClient()
) {

    suspend fun loadTopHeadlines(limit: Int = 15): HeadlinesResult = coroutineScope {
        val failed = mutableListOf<String>()

        val perSource = sources.map { source ->
            async(Dispatchers.IO) {
                try {
                    fetchAndParse(source)
                } catch (e: Exception) {
                    synchronized(failed) { failed.add(source.name) }
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        val ranked = rank(perSource, limit)
        HeadlinesResult(articles = ranked, failedSources = failed)
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
            return if (source.aiFilterNeeded) raw.filter(::isAiRelevant) else raw
        }
    }

    private fun isAiRelevant(article: Article): Boolean {
        val haystack = " ${article.title.lowercase()} ${article.summary?.lowercase().orEmpty()} "
        return NewsSources.AI_KEYWORDS.any { haystack.contains(it) }
    }

    /**
     * Merge rule: de-duplicate by [Article.dedupeKey], sort newest-first (undated
     * items last), then apply a per-source cap so the final [limit] stays diverse.
     */
    private fun rank(articles: List<Article>, limit: Int): List<Article> {
        val deduped = articles
            .filter { it.dedupeKey.isNotBlank() }
            .associateBy { it.dedupeKey }  // last write wins; acceptable for dupes
            .values
            .sortedByDescending { it.publishedAtMillis ?: Long.MIN_VALUE }

        val maxPerSource = maxOf(2, (limit / 3))
        val counts = mutableMapOf<String, Int>()
        val diverse = mutableListOf<Article>()
        for (a in deduped) {
            val c = counts.getOrDefault(a.sourceName, 0)
            if (c < maxPerSource) {
                diverse.add(a)
                counts[a.sourceName] = c + 1
            }
            if (diverse.size == limit) break
        }
        // If the cap left us short (few sources responded), backfill from the rest.
        if (diverse.size < limit) {
            for (a in deduped) {
                if (diverse.size == limit) break
                if (a !in diverse) diverse.add(a)
            }
        }
        return diverse
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
