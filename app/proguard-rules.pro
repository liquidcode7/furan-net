# FURAN ProGuard rules

# Keep Hilt entry points
-keepclassmembers class * {
    @dagger.hilt.* <methods>;
}

# Keep WorkManager workers
-keep class com.liquidfuran.furan.worker.** { *; }

# Keep serializable models
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class kotlinx.serialization.** { *; }

# OkHttp / Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature

# DataStore
-keep class androidx.datastore.** { *; }

# ZXing
-keep class com.google.zxing.** { *; }
