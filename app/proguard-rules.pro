# GuardianTrack ProGuard / R8 Hardening Rules

# ---------------------------------------------------------
# R8 / ProGuard Optimization & Obfuscation
# ---------------------------------------------------------
-repackageclasses 'com.guardiantrack.obf'
-allowaccessmodification
-overloadaggressively

# Stacktrace De-obfuscation Preservation without leaking original source filenames
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ---------------------------------------------------------
# Kotlin Coroutines
# ---------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ---------------------------------------------------------
# Kotlinx Serialization
# ---------------------------------------------------------
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------
# Room Database & SQLCipher
# ---------------------------------------------------------
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ---------------------------------------------------------
# AndroidX Security Crypto & Keystore
# ---------------------------------------------------------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ---------------------------------------------------------
# Google Play Services Location & Geofencing
# ---------------------------------------------------------
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.location.**

# ---------------------------------------------------------
# Retrofit & OkHttp
# ---------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# ---------------------------------------------------------
# AndroidX WorkManager Hilt Integration
# ---------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------------------------------------------------------
# Dagger Hilt
# ---------------------------------------------------------
-keep class * extends dagger.hilt.internal.UnsafeCasts { *; }
-dontwarn dagger.hilt.**
