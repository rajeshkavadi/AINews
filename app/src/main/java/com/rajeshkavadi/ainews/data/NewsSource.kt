package com.rajeshkavadi.ainews.data

/**
 * A feed the app pulls from.
 *
 * @param name           Display name shown on each headline card.
 * @param feedUrl        RSS 2.0 or Atom feed URL. The parser auto-detects format.
 * @param aiFilterNeeded Whether items must pass an AI keyword filter to be kept.
 *                       Set false for feeds that are already AI-only (so we don't
 *                       accidentally drop a relevant item whose title lacks an
 *                       obvious keyword); set true for broad/general-tech feeds.
 */
data class NewsSource(
    val name: String,
    val feedUrl: String,
    val aiFilterNeeded: Boolean
)

/**
 * The source registry — the single place to add, remove, or re-point feeds.
 *
 * Notes on the user's original six sources:
 *  - Analytics Insight, MachineLearningMastery, DevX, AI Magazine expose RSS and
 *    are included.
 *  - OpenAI Stories (openai.com/stories) and aitrendz.xyz do NOT publish a
 *    reliable public feed, so they cannot be aggregated on-device without HTML
 *    scraping (fragile + ToS-sensitive). They are intentionally omitted.
 *  - Additional reputable AI feeds are added so "top 15 across the world" has
 *    enough geographic and topical spread to be meaningful.
 *
 * If a feed URL 404s or changes, only that source degrades — the rest still load.
 */
object NewsSources {
    val ALL: List<NewsSource> = listOf(
        // ---- From the user's original list (feeds that actually exist) ----
        NewsSource("Analytics Insight", "https://www.analyticsinsight.net/feed/", aiFilterNeeded = true),
        NewsSource("MachineLearningMastery", "https://machinelearningmastery.com/feed/", aiFilterNeeded = false),
        NewsSource("DevX", "https://www.devx.com/feed/", aiFilterNeeded = true),
        NewsSource("AI Magazine", "https://aimagazine.com/rss/", aiFilterNeeded = false),

        // ---- Added for global coverage and reliability ----
        NewsSource("VentureBeat AI", "https://venturebeat.com/category/ai/feed/", aiFilterNeeded = false),
        NewsSource("MIT Tech Review AI", "https://www.technologyreview.com/topic/artificial-intelligence/feed", aiFilterNeeded = false),
        NewsSource("The Verge AI", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml", aiFilterNeeded = false),
        NewsSource("Google Research", "https://research.google/blog/rss/", aiFilterNeeded = true),
        NewsSource("Hugging Face", "https://huggingface.co/blog/feed.xml", aiFilterNeeded = false)
    )

    /**
     * Keywords used to keep only AI-relevant items from broad feeds.
     * Deliberately conservative to avoid false positives (e.g. "aid", "brain").
     */
    val AI_KEYWORDS: List<String> = listOf(
        "artificial intelligence", " ai ", " ai,", " ai.", " ai:", "ai-", "genai",
        "machine learning", "deep learning", "neural network", "llm", "large language model",
        "generative", "chatgpt", "openai", "anthropic", "claude", "gemini", "llama",
        "transformer", "diffusion model", "computer vision", "nlp", "agentic", "agent",
        "foundation model", "gpt", "mistral", "deepseek", "inference", "fine-tun"
    )
}
