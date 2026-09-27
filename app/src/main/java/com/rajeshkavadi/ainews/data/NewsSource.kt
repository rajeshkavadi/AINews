package com.rajeshkavadi.ainews.data

/**
 * A feed the app pulls from.
 *
 * @param name           Display name shown on each headline card.
 * @param feedUrl        RSS 2.0 or Atom feed URL. The parser auto-detects format.
 * @param filterKeywords If non-null, only items whose title/summary contain at
 *                       least one of these (case-insensitive) are kept. Use it
 *                       for broad feeds; leave null for already-on-topic feeds.
 */
data class NewsSource(
    val name: String,
    val feedUrl: String,
    val filterKeywords: List<String>? = null
)

/**
 * The source registry — the single place to add, remove, or re-point feeds.
 * Grouped per tab (see [Category]). If a feed URL dies or changes, only that
 * source degrades; the rest still load.
 *
 * NOTE: keyword sets are declared before the source lists on purpose — an
 * `object`'s properties initialize top-to-bottom, so a source list must not
 * reference a keyword list defined below it.
 */
object NewsSources {

    // ------------------------------------------------------------- Keyword sets

    /** Keeps AI-relevant items from broad feeds. Conservative to avoid false hits. */
    val AI_KEYWORDS: List<String> = listOf(
        "artificial intelligence", " ai ", " ai,", " ai.", " ai:", "ai-", "genai",
        "machine learning", "deep learning", "neural network", "llm", "large language model",
        "generative", "chatgpt", "openai", "anthropic", "claude", "gemini", "llama",
        "transformer", "diffusion model", "computer vision", "nlp", "agentic", "agent",
        "foundation model", "gpt", "mistral", "deepseek", "inference", "fine-tun"
    )

    /** Keeps startup-relevant items from broad business/tech feeds. */
    val STARTUP_KEYWORDS: List<String> = listOf(
        "startup", "start-up", "funding", "raises", "raised", "seed round", "pre-seed",
        "series a", "series b", "series c", "venture", " vc ", "founder", "co-founder",
        "valuation", "unicorn", "acqui", "ipo", "angel", "incubator", "accelerator",
        "y combinator", "bootstrapp", "term sheet", "cap table"
    )

    /**
     * Names/terms that identify a space *company* story, used to prioritize the
     * space slots so government/agency-only items rank below company news.
     */
    val SPACE_COMPANY_KEYWORDS: List<String> = listOf(
        "spacex", "starship", "skyroot", "agnikul", "dhruva space", "pixxel",
        "rocket lab", "blue origin", "relativity space", "firefly", "stoke space",
        "sierra space", "astra ", "planet labs", "axiom space", "intuitive machines",
        "isro", "startup", "start-up", "private", "launch vehicle", "smallsat",
        "cubesat", "reusable rocket", "satellite constellation"
    )

    // ---------------------------------------------------------------- AI tab
    val AI_SOURCES: List<NewsSource> = listOf(
        // From the user's original list (feeds that actually exist):
        NewsSource("Analytics Insight", "https://www.analyticsinsight.net/feed/", AI_KEYWORDS),
        NewsSource("MachineLearningMastery", "https://machinelearningmastery.com/feed/"),
        NewsSource("DevX", "https://www.devx.com/feed/", AI_KEYWORDS),
        NewsSource("AI Magazine", "https://aimagazine.com/rss/"),
        // Added for global coverage / reliability:
        NewsSource("VentureBeat AI", "https://venturebeat.com/category/ai/feed/"),
        NewsSource("MIT Tech Review AI", "https://www.technologyreview.com/topic/artificial-intelligence/feed"),
        NewsSource("The Verge AI", "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"),
        NewsSource("Google Research", "https://research.google/blog/rss/", AI_KEYWORDS),
        NewsSource("Hugging Face", "https://huggingface.co/blog/feed.xml")
    )

    // --------------------------------------------------- Start-ups tab: general
    // Global spread: US, Europe, and India, biased toward funding/founder news.
    val STARTUP_GENERAL_SOURCES: List<NewsSource> = listOf(
        NewsSource("TechCrunch Startups", "https://techcrunch.com/category/startups/feed/"),
        NewsSource("Crunchbase News", "https://news.crunchbase.com/feed/"),
        NewsSource("EU-Startups", "https://www.eu-startups.com/feed/"),
        NewsSource("Tech.eu", "https://tech.eu/feed/", STARTUP_KEYWORDS),
        NewsSource("YourStory", "https://yourstory.com/feed", STARTUP_KEYWORDS),
        NewsSource("Inc42", "https://inc42.com/feed/"),
        NewsSource("Entrackr", "https://entrackr.com/feed/")
    )

    // ---------------------------------------------------- Start-ups tab: space
    // Dedicated space-news feeds; the repository prioritizes items that name an
    // actual space company (see SPACE_COMPANY_KEYWORDS) for the ~5 space slots.
    val STARTUP_SPACE_SOURCES: List<NewsSource> = listOf(
        NewsSource("SpaceNews", "https://spacenews.com/feed/"),
        NewsSource("Space.com", "https://www.space.com/feeds/all"),
        NewsSource("Ars Technica Space", "https://arstechnica.com/space/feed/")
    )
}
