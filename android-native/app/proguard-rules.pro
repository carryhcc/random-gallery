# Keep default proguard rules.

# --- Moshi / 数据模型 ---
# 数据模型用反射/codegen 进行 JSON 解析，避免 R8 改名导致 Release 解析失败
-keep class com.example.randomgallery.android.data.model.** { *; }
-keepclassmembers class com.example.randomgallery.android.data.model.** { *; }

# Moshi 自身（保留泛型签名与注解，保证适配器查找正常）
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.moshi.** { *; }
-keep class kotlin.Metadata { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit
-keepattributes Exceptions
-keep,allowobfuscation interface retrofit2.Call
-keep,allowobfuscation class retrofit2.Response
