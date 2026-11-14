# AfyaQuest shrinker (R8) rules.
#
# Only what R8 cannot work out by itself is kept: classes that are filled or created by
# reflection. Every library used here (Compose, Navigation, Hilt, Room, WorkManager, OkHttp,
# Retrofit, Maps, ExoPlayer, ML Kit, coroutines, DataStore) ships its own rules.
#
# This file used to keep whole libraries ("-keep class androidx.compose.** { *; }" and the like).
# That switched shrinking and optimisation off for most of the app: all 11,000 Material icons were
# packaged, and the release code was 27 MB. Do not add a library-wide keep back to fix a
# release-only crash; keep the one class that is reached by reflection instead.

# Readable stack traces in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Needed by Gson and Retrofit to read generic types and annotations at run time.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault

# ---- Classes Gson fills by reflection (API bodies, cached JSON, bundled JSON assets) ----
-keep class com.afyaquest.app.domain.model.** { *; }
-keep class com.afyaquest.app.data.local.entity.** { *; }
-keep class com.afyaquest.app.data.remote.dto.** { *; }
-keep class com.afyaquest.app.data.remote.TokenAuthenticator$* { *; }
# Emergency Response Guide: the triage tree, audio manifest and case path are read with Gson by
# reflection. Without this R8 renames/merges them and the guide crashes on open in release builds
# ("Unable to invoke no-args constructor ... Abstract class can't be instantiated").
-keep class com.afyaquest.app.domain.triage.** { *; }

# ---- Gson (2.8.x has no rules of its own) ----
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# R8 full mode strips the generic signature from anonymous TypeToken subclasses unless kept.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ---- Retrofit with R8 full mode (in addition to the rules Retrofit ships) ----
# The API interface keeps its full generic signatures.
-keep interface com.afyaquest.app.data.remote.ApiService { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
# Retrofit @suspend: the response type is carried on Continuation; without this the cast to
# ParameterizedType fails in release builds only (ClassCastException on login, etc.).
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- WorkManager stores worker class names; they must survive from one version to the next ----
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}

# ---- Remove debug logging from release builds ----
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
