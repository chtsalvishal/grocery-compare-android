# ── Kotlin Serialization ──────────────────────────────────────────────────────
# R8 full mode strips generated $serializer classes; these rules prevent that.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# Keep companion objects that hold serializers
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
# Keep KSerializer factory methods on companion objects
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep object classes that carry a serializer
-if @kotlinx.serialization.Serializable class **
-keepclasseswithmembers class <1> {
    public static ** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep generated $serializer inner classes
-keep class **$$serializer { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep class * { @androidx.room.Entity *; }
-keep class * { @androidx.room.Dao *; }

# ── Ktor ──────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }

# ── Timber ────────────────────────────────────────────────────────────────────
-keep class timber.log.** { *; }

# ── App data classes ──────────────────────────────────────────────────────────
-keep class com.example.grocerycompare.data.local.entity.** { *; }
-keep class com.example.grocerycompare.data.remote.ApiProduct { *; }
-keep class com.example.grocerycompare.data.repository.** { *; }

# ── Suppress warnings for optional / unused transitive deps ───────────────────
-dontwarn org.jspecify.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn kotlin.annotations.jvm.**
-dontwarn com.sun.jna.**
