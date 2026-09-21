# OkHttp / Okio ship their own consumer rules; nothing custom is required here.
# Keep our data models used with reflection-free code as-is. Add app-specific
# keep rules below if you introduce serialization.
-dontwarn okhttp3.**
-dontwarn okio.**
