package com.rajeshkavadi.ainews.data

/**
 * A single normalized news item, independent of whether the origin feed was
 * RSS 2.0 or Atom.
 *
 * [publishedAtMillis] is epoch millis, or null when the feed omitted a parseable
 * date. Items without a date are pushed to the bottom of the ranking rather than
 * dropped, because a missing date is not evidence the item is old.
 */
data class Article(
    val title: String,
    val link: String,
    val sourceName: String,
    val summary: String?,
    val imageUrl: String?,
    val publishedAtMillis: Long?
) {
    /**
     * Key used for cross-source de-duplication. The same story syndicated on two
     * sites rarely shares a URL, so we normalize the title instead: lowercase,
     * strip non-alphanumerics, collapse whitespace. This is a heuristic, not a
     * guarantee — near-identical rewrites can still slip through.
     */
    val dedupeKey: String
        get() = title
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
