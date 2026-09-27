package com.rajeshkavadi.ainews.data

/** A top-level tab in the app. [AI] is the default. */
enum class Category(val title: String, val subtitle: String) {
    AI("AI", "Top 15 · latest AI headlines worldwide"),
    STARTUPS("Start-ups", "Top 15 · global startups (incl. ~5 space)")
}
