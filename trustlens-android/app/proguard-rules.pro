# Retrofit & Gson rules
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Preserve TRUSTLENS data models for serialization
-keep class com.trustlens.app.data.model.** { *; }
