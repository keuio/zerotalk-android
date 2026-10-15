# =============================================================================
# ZeroTalk R8 / ProGuard 规则
#
# release 构建已启用 R8（isMinifyEnabled + isShrinkResources）。
# 本文件只保留「被反射 / 被 JNI / 被框架按名字访问」的代码，其余交给 R8 裁剪。
#
# 排查方法：本文件的每一条规则都对应源码中的一处反射访问点，改动前请先确认
# 该访问点是否仍然存在。
# =============================================================================


# -----------------------------------------------------------------------------
# 0. 通用属性保留
# -----------------------------------------------------------------------------
# 【必须】Signature：
#   Gson 的 TypeToken 通过 getGenericSuperclass() 反射读取类型参数，
#   本项目有约 110 处 `object : TypeToken<ApiResponse<XxxData>>() {}.type`。
#   一旦 Signature 被裁掉，fromJson 会退化成原始类型解析 —— 不抛异常，只返回
#   字段全为默认值的对象（静默数据损坏），是本项目最危险的一类混淆事故。
-keepattributes Signature

# 注解：Gson 读 @SerializedName；Compose / Kotlin 元数据也依赖注解
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# 崩溃栈可读性（体积代价很小，线上排查价值很高）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# -----------------------------------------------------------------------------
# 1. Gson
# -----------------------------------------------------------------------------
# 1.1 TypeToken 及其子类（含匿名子类）：类本身必须保留
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# 1.2 带 @SerializedName 的字段：允许改名（Gson 读注解取 JSON key），
#     但字段本身不能被裁掉
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 1.3 【关键】未标注 @SerializedName 的字段由 Gson 按「Kotlin 字段名」映射。
#     实测本项目有 129 个类、676 个这样的字段，分布在 data.*、ui.* 与
#     androidMain（例如 RawChatMessage、OtherUserProfile、MomentItem、Message、
#     VoiceCall、MbtiResult、MatchingPreferences 等）。
#     逐个枚举极易漏，故统一保留全工程字段名。
#     代价：字段名保留在字符串常量池（约数十 KB），且 R8 不再裁剪无用字段；
#     不影响类与方法的裁剪/混淆，也就是不影响本次体积优化的主要收益。
#     TODO: 后续可用注解统一标注 DTO 后收紧为 -keepclassmembers class top.lanxint.zerotalk.data.**
-keepclassmembers class ** { <fields>; }

# 1.4 自定义 TypeAdapter / TypeAdapterFactory 由 GsonBuilder 注册，
#     其 create() 由 Gson 回调
-keep class top.lanxint.zerotalk.data.network.*TypeAdapter* { *; }


# -----------------------------------------------------------------------------
# 2. WebRTC（io.github.webrtc-sdk:android:144.7559.15）
# -----------------------------------------------------------------------------
# 【必须】该 AAR 不自带 consumer proguard 规则，且大量方法由 JNI / native
# 按名字回调（PeerConnection.Observer、SdpObserver、AudioDeviceModule 等）。
# 被裁掉或改名会导致语音通话直接崩溃（UnsatisfiedLinkError / NoSuchMethodError）。
-keep class org.webrtc.** { *; }
-keep interface org.webrtc.** { *; }
-dontwarn org.webrtc.**


# -----------------------------------------------------------------------------
# 3. Kotlin / 协程
# -----------------------------------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin 反射（::class / KClass）元数据
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}


# -----------------------------------------------------------------------------
# 4. OkHttp / Okio（自带 META-INF/proguard/okhttp3.pro，此处为兜底与可选依赖）
# -----------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**


# -----------------------------------------------------------------------------
# 5. 应用入口（AGP 会依据 AndroidManifest 自动保留，此处显式声明以防 manifest
#    合并顺序变化；同时让 ZeroTalkApplication.onCreate 的注入点保持稳定）
# -----------------------------------------------------------------------------
-keep class top.lanxint.zerotalk.ZeroTalkApplication { *; }
-keep class top.lanxint.zerotalk.MainActivity { *; }
-keep class top.lanxint.zerotalk.notify.ZeroTalkKeepAliveService { *; }


# -----------------------------------------------------------------------------
# 6. Compose
# -----------------------------------------------------------------------------
# androidx.compose 各 artifact 自带 consumer 规则，这里仅补充：
# 组合函数可能被 Compose 运行时按签名查找
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.**
