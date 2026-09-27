package com.rajeshkavadi.ainews.data

/**
 * A top-level tab in the app. [AI] is the default.
 *
 * [live] distinguishes the two mechanisms: AI and STARTUPS are live RSS feeds
 * ranked by recency; PODCASTS is a curated, bundled list ("best & useful", not
 * latest) and does not hit the network.
 */
enum class Category(val title: String, val subtitle: String, val live: Boolean) {
    AI("AI", "Top 15 · latest AI headlines worldwide", live = true),
    STARTUPS("Start-ups", "Top 15 · global startups (incl. ~5 space)", live = true),
    PODCASTS("Podcasts/TED", "15 curated best & useful · incl. neuro/psychology", live = false)
}
