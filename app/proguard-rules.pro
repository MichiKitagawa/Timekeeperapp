# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# Gson/Retrofit ProGuard Rules
# ========================================

# Gson uses generic type information stored in a class file when working with fields.
# Proguard removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# Keep generic type information for Gson
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Application classes that will be serialized/deserialized over Gson
-keep class com.example.timekeeper.data.api.** { <fields>; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep annotation default values (e.g. retrofit2.http.Field.encoded)
-keepattributes AnnotationDefault

# Keep the @SerializedName annotation
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# ========================================
# Timekeeper Specific Rules
# ========================================

# Keep API data classes structure
-keep class com.example.timekeeper.data.api.CreateCheckoutSessionRequest { *; }
-keep class com.example.timekeeper.data.api.CreateCheckoutSessionResponse { *; }
-keep class com.example.timekeeper.data.api.LicenseConfirmRequest { *; }
-keep class com.example.timekeeper.data.api.LicenseConfirmResponse { *; }
-keep class com.example.timekeeper.data.api.UnlockDaypassRequest { *; }
-keep class com.example.timekeeper.data.api.UnlockDaypassResponse { *; }
-keep class com.example.timekeeper.data.api.ErrorResponse { *; }

# Keep Hilt components
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep accessibility service
-keep class com.example.timekeeper.service.MyAccessibilityService { *; }

# Keep Room database classes
-keep class com.example.timekeeper.data.database.** { *; }

# ========================================
# Security & WebView Rules
# ========================================

# Keep WebView JavaScript interface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}