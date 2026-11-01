# Kotlin IDE ProGuard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
