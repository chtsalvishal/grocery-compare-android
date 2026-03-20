# Keep Room
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.Dao *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

# Keep ML Kit Text Recognition
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }

# Keep Jsoup
-keep class org.jsoup.** { *; }

# Timber
-keep class timber.log.** { *; }

# Application Specific
-keep class com.example.grocerycompare.data.local.entity.** { *; }
-keep class com.example.grocerycompare.data.repository.** { *; }

# Ignore warnings for missing optional annotation classes
-dontwarn org.jspecify.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**

# PostgreSQL JDBC pulled in transitively — not used on Android, suppress R8 warnings
-dontwarn com.sun.jna.**
-dontwarn org.postgresql.**

# Resilience4j + JVM management — server-side libs pulled in transitively, not used on Android
-dontwarn io.github.resilience4j.bulkhead.Bulkhead
-dontwarn io.github.resilience4j.bulkhead.BulkheadConfig
-dontwarn io.github.resilience4j.circuitbreaker.CircuitBreaker
-dontwarn io.github.resilience4j.micrometer.Timer$Context
-dontwarn io.github.resilience4j.micrometer.Timer
-dontwarn io.github.resilience4j.timelimiter.TimeLimiter
-dontwarn io.github.resilience4j.timelimiter.TimeLimiterConfig
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean
