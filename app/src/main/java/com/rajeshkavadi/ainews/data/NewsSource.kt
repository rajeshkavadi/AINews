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

    // -------------------------------------------------- Podcasts/TED tab (curated)
    // A hand-picked, timeless list of 15 — "best & useful", not latest. 8 of the
    // 15 are neuroscience/psychology (exceeding the >=5 ask). Edit this list to
    // change the picks; it ships in the app and needs no network.
    // Ordering here is the display order (curated, not recency-ranked).
    val CURATED_PODCASTS: List<Article> = listOf(
        pick(
            "Huberman Lab — practical neuroscience for daily life",
            "https://www.hubermanlab.com/",
            "Podcast · Andrew Huberman",
            "Stanford neuroscientist on sleep, focus, dopamine and habits — with actionable protocols."
        ),
        pick(
            "Your brain hallucinates your conscious reality",
            "https://www.ted.com/talks/anil_seth_your_brain_hallucinates_your_conscious_reality",
            "TED · Anil Seth",
            "Neuroscience of consciousness: how the brain constructs the reality you experience."
        ),
        pick(
            "The brain-changing benefits of exercise",
            "https://www.ted.com/talks/wendy_suzuki_the_brain_changing_benefits_of_exercise",
            "TED · Wendy Suzuki",
            "How movement reshapes memory, mood and the aging brain."
        ),
        pick(
            "What makes a good life? Lessons from the longest study on happiness",
            "https://www.ted.com/talks/robert_waldinger_what_makes_a_good_life_lessons_from_the_longest_study_on_happiness",
            "TED · Robert Waldinger",
            "75-year Harvard study on what actually predicts a happy, healthy life."
        ),
        pick(
            "The power of vulnerability",
            "https://www.ted.com/talks/brene_brown_the_power_of_vulnerability",
            "TED · Brené Brown",
            "Landmark psychology talk on connection, shame and courage."
        ),
        pick(
            "How to make stress your friend",
            "https://www.ted.com/talks/kelly_mcgonigal_how_to_make_stress_your_friend",
            "TED · Kelly McGonigal",
            "Reframing the psychology of stress to make it work for you."
        ),
        pick(
            "Your body language may shape who you are",
            "https://www.ted.com/talks/amy_cuddy_your_body_language_may_shape_who_you_are",
            "TED · Amy Cuddy",
            "Psychology of posture, presence and confidence."
        ),
        pick(
            "Hidden Brain — the unconscious patterns that drive behavior",
            "https://hiddenbrain.org/",
            "Podcast · Shankar Vedantam",
            "Behavioral science and psychology behind why we do what we do."
        ),
        pick(
            "Lex Fridman Podcast — long-form science & technology",
            "https://lexfridman.com/podcast/",
            "Podcast · Lex Fridman",
            "Deep conversations with scientists, engineers and founders on AI and the mind."
        ),
        pick(
            "The Tim Ferriss Show — tools and routines of top performers",
            "https://tim.blog/podcast/",
            "Podcast · Tim Ferriss",
            "Deconstructing the habits, tactics and decisions of world-class performers."
        ),
        pick(
            "How I Built This — the stories behind great companies",
            "https://www.npr.org/podcasts/510313/how-i-built-this",
            "Podcast · Guy Raz (NPR)",
            "Founders on how they built their startups, in their own words."
        ),
        pick(
            "Do schools kill creativity?",
            "https://www.ted.com/talks/sir_ken_robinson_do_schools_kill_creativity",
            "TED · Sir Ken Robinson",
            "The most-watched TED talk ever — on creativity and how we learn."
        ),
        pick(
            "How great leaders inspire action",
            "https://www.ted.com/talks/simon_sinek_how_great_leaders_inspire_action",
            "TED · Simon Sinek",
            "\"Start with why\" — the psychology of leadership and motivation."
        ),
        pick(
            "Freakonomics Radio — the hidden side of everything",
            "https://freakonomics.com/series/freakonomics-radio/",
            "Podcast · Stephen Dubner",
            "Economics and incentives applied to everyday questions."
        ),
        pick(
            "Radiolab — science, storytelling and big questions",
            "https://radiolab.org/",
            "Podcast · WNYC",
            "Award-winning science storytelling that reshapes how you see the world."
        )
    )

    /** Small builder for curated items (no date/image; curated order is kept). */
    private fun pick(title: String, link: String, source: String, why: String) = Article(
        title = title,
        link = link,
        sourceName = source,
        summary = why,
        imageUrl = null,
        publishedAtMillis = null
    )
}
