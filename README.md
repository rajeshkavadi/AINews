# AI News (Android)

A self-contained Android app that aggregates the latest **AI headlines from
around the world** and shows the top 15 in a clean, tappable list. No backend,
no API keys, no running costs — the app fetches RSS/Atom feeds directly on the
device, deduplicates across sources, and ranks the result.

## What this is (and what it honestly is not)

**It is:** an on-device feed aggregator. On launch (and on pull-to-refresh) it
fetches every configured feed in parallel, parses RSS 2.0 and Atom, filters
broad feeds down to AI-relevant items, removes cross-source duplicates, and
shows the newest 15 with a per-source cap for diversity.

**It is not** an editorially "most important" ranking. Ranking *global
importance* on-device is not possible without a scoring backend or an LLM layer,
so "Top 15" here means **the 15 most recent AI headlines** across the sources,
with no single source allowed to dominate. The UI labels this accurately
("latest AI headlines worldwide"). If you later want true importance ranking,
that's a backend feature — see *Roadmap*.

### Sources

From the originally requested list, these expose machine-readable feeds and are
included: **Analytics Insight, MachineLearningMastery, DevX, AI Magazine**.

Two requested sources — **OpenAI Stories** (`openai.com/stories`) and
**aitrendz.xyz** — do **not** publish a reliable public RSS/Atom feed. Pulling
them would require HTML scraping, which is fragile (breaks on any markup change),
battery-hostile on a phone, and ToS-sensitive. They are intentionally omitted.

To keep global/topical coverage meaningful, these reputable AI feeds are added:
**VentureBeat AI, MIT Technology Review AI, The Verge AI, Google Research,
Hugging Face**.

All sources live in one file — `app/src/main/java/.../data/NewsSource.kt` — so
adding, removing, or re-pointing a feed is a one-line change. If a feed URL dies
or changes, only that source degrades; the rest still load, and the app tells the
user which sources didn't respond.

## Architecture

- **Language / UI:** Kotlin + Jetpack Compose (Material 3, dynamic color).
- **Pattern:** MVVM — `HeadlinesViewModel` exposes an immutable
  `HeadlinesUiState` via `StateFlow`.
- **Networking:** OkHttp; all feeds fetched concurrently on `Dispatchers.IO`.
- **Parsing:** Android's built-in `XmlPullParser` (zero extra dependency),
  handling RSS 2.0 and Atom in one pass, tolerant of missing fields.
- **Ranking:** dedupe by normalized title → sort by recency (undated last) →
  per-source cap → take 15, with backfill if few sources respond.
- **Article view:** opens in a Chrome Custom Tab (no bundled WebView baggage).

```
app/src/main/java/com/rajeshkavadi/ainews/
├── MainActivity.kt
├── data/
│   ├── Article.kt          # normalized model + dedupe key
│   ├── NewsSource.kt       # the source registry (edit feeds here)
│   ├── RssParser.kt        # RSS 2.0 + Atom parser
│   └── NewsRepository.kt   # concurrent fetch, filter, dedupe, rank
└── ui/
    ├── HeadlinesViewModel.kt
    ├── HeadlinesScreen.kt
    └── theme/Theme.kt
```

## Build & run

Requires Android Studio (Ladybug or newer) or a local Android SDK.

```bash
./gradlew assembleDebug          # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # build + install on a connected device/emulator
./gradlew testDebugUnitTest      # run JVM unit tests
```

Or just open the project in Android Studio and hit Run.

- `minSdk 24`, `targetSdk 35`, `compileSdk 35`
- AGP 8.7.3, Kotlin 2.0.21, Gradle 8.11.1 (wrapper committed)

> **Build not verified in CI/this environment.** This project was authored in a
> sandbox where Google's Maven (`dl.google.com`) is network-blocked, so the
> Android toolchain and AndroidX artifacts could not be downloaded and a full
> `gradle` build could not be executed here. The code has been reviewed for
> compile correctness, but **run `./gradlew assembleDebug` locally to confirm**
> before relying on it.

## Testing

`ArticleDedupeTest` covers the cross-source de-duplication key on the plain JVM.
The RSS parser depends on `android.util.Xml`, so testing it needs an instrumented
or Robolectric test rather than a local unit test — noted in the test file.

## Known limitations / failure modes

- Recency ranking ≠ importance ranking (by design; see above).
- Title-based dedupe can miss heavily reworded syndications.
- No offline cache yet — a cold start with no network shows an error + retry.
- Feed availability is outside our control; a source can rate-limit or 404.

## Roadmap (if you want to go further)

1. **Offline cache** (Room/DataStore) so the last load survives no-network.
2. **Backend aggregator** for real importance ranking, clustering, and to safely
   incorporate no-RSS sources (OpenAI, aitrendz) server-side.
3. **Categories/filters** (research vs. product vs. policy) and search.
4. **Robolectric tests** for the parser against captured feed fixtures.
