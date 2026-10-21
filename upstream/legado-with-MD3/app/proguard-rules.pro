# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*

-flattenpackagehierarchy

#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################
# 屏蔽错误Unresolved class name
#noinspection ShrinkerUnresolvedReference

# 移除Log类打印各个等级日志的代码，打正式包的时候可以做为禁log使用，这里可以作为禁止log打印的功能使用
# 记得proguard-android.txt中一定不要加-dontoptimize才起作用
# 另外的一种实现方案是通过BuildConfig.DEBUG的变量来控制
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 保持js引擎调用的java类
-keep class * extends io.legado.app.help.JsExtensions{*;}
# 数据类
-keep class **.data.entities.**{*;}
# Gson反序列化用的数据传输类
-keep class io.legado.app.model.translation.**{*;}
-keep class io.legado.app.domain.model.DictPair{*;}
-keep class io.legado.app.domain.model.BookDictionary{*;}
-keep class io.legado.app.domain.model.TextChunk{*;}
-keep class io.legado.app.domain.model.ModuleDef{*;}
-keep class io.legado.app.domain.model.ModuleItem{*;}
-keep class io.legado.app.domain.model.CustomSetItem{*;}
-keep class io.legado.app.data.repository.GoogleTranslateResponse{*;}
-keep class io.legado.app.data.repository.GoogleSentence{*;}
-keep class io.legado.app.data.repository.GoogleSpell{*;}
-keep class io.legado.app.data.repository.OpenAIResponse{*;}
-keep class io.legado.app.data.repository.OpenAIChoice{*;}
-keep class io.legado.app.data.repository.OpenAIMessage{*;}
# 缓存 Cookie
-keep class **.help.http.CookieStore{*;}
-keep class **.help.CacheManager{*;}
# StrResponse
-keep class **.help.http.StrResponse{*;}

# markwon
-dontwarn org.commonmark.ext.gfm.**

# hutool-core hutool-crypto（书源 JS 通过 Packages.cn.hutool.* 反射调用，需整体保留）
-keep class cn.hutool.crypto.**{*;}
-keep class cn.hutool.core.codec.**{*;}
-keep class cn.hutool.core.util.**{*;}
-dontwarn cn.hutool.**

-keep class okhttp3.*{*;}
-keep class okio.*{*;}
-keep class com.jayway.jsonpath.*{*;}

# LiveEventBus
-keepclassmembers class androidx.lifecycle.LiveData {
    *** mObservers;
    *** mActiveCount;
}
-keepclassmembers class androidx.arch.core.internal.SafeIterableMap {
    *** size();
    *** putIfAbsent(...);
}

## ChangeBookSourceDialog initNavigationView
-keepclassmembers class androidx.appcompat.widget.Toolbar {
    *** mNavButtonView;
}

# FileDocExtensions.kt treeDocumentFileConstructor
-keep class androidx.documentfile.provider.TreeDocumentFile {
    <init>(...);
}

# JsoupXpath
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.AxisSelector{*;}
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.NodeTest{*;}
-keep,allowobfuscation class * implements org.seimicrawler.xpath.core.Function{*;}

## JSOUP
-keep class org.jsoup.**{*;}
-dontwarn org.jspecify.annotations.NullMarked

## ExoPlayer 反射设置ua 保证该私有变量不被混淆
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}
## ExoPlayer 如果还不能播放就取消注释这个
# -keep class com.google.android.exoplayer2.** {*;}

## 对外提供api
-keep class io.legado.app.api.ReturnData{*;}

# Cronet
-keepclassmembers class org.chromium.net.X509Util {
    *** sDefaultTrustManager;
    *** sTestTrustManager;
}

# Throwable
-keepnames class * extends java.lang.Throwable
-keepclassmembernames,allowobfuscation class * extends java.lang.Throwable{*;}
# 忽略 Ktor 在 Android 上对 Java SE 管理类的引用
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.IntellijIdeaDebugDetector
-keep,allowobfuscation class io.ktor.util.debug.** { *; }

## ==== SPEC-148 build-fix: R8 missing classes ====
## Firebase/google-services removal (working tree) took play-services-tasks & co
## off the classpath; the bundled Chromium AAR (webview chromium, cronet,
## sms/webauthn wrappers) still references them at compile time. Runtime paths
## guard these optional deps; suppress the compile-time warnings only.
# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn android.webkit.WebViewFactoryProvider
-dontwarn com.google.android.gms.auth.api.phone.SmsCodeBrowserClient
-dontwarn com.google.android.gms.auth.api.phone.SmsCodeRetriever
-dontwarn com.google.android.gms.auth.api.phone.SmsRetriever
-dontwarn com.google.android.gms.auth.api.phone.SmsRetrieverClient
-dontwarn com.google.android.gms.common.GoogleApiAvailability
-dontwarn com.google.android.gms.common.api.Api$AbstractClientBuilder
-dontwarn com.google.android.gms.common.api.Api$ApiOptions$NoOptions
-dontwarn com.google.android.gms.common.api.Api$ApiOptions
-dontwarn com.google.android.gms.common.api.Api$ClientKey
-dontwarn com.google.android.gms.common.api.Api
-dontwarn com.google.android.gms.common.api.ApiException
-dontwarn com.google.android.gms.common.api.GoogleApi
-dontwarn com.google.android.gms.common.api.Status
-dontwarn com.google.android.gms.common.api.internal.ApiExceptionMapper
-dontwarn com.google.android.gms.common.api.internal.RemoteCall
-dontwarn com.google.android.gms.common.api.internal.StatusExceptionMapper
-dontwarn com.google.android.gms.common.api.internal.TaskApiCall$Builder
-dontwarn com.google.android.gms.common.api.internal.TaskApiCall
-dontwarn com.google.android.gms.identitycredentials.CreateCredentialRequest
-dontwarn com.google.android.gms.identitycredentials.IdentityCredentialClient
-dontwarn com.google.android.gms.identitycredentials.IdentityCredentialManager$Companion
-dontwarn com.google.android.gms.identitycredentials.IdentityCredentialManager
-dontwarn com.google.android.gms.identitycredentials.SignalCredentialStateRequest
-dontwarn com.google.android.gms.location.FusedLocationProviderClient
-dontwarn com.google.android.gms.location.LocationCallback
-dontwarn com.google.android.gms.location.LocationRequest$Builder
-dontwarn com.google.android.gms.location.LocationRequest
-dontwarn com.google.android.gms.location.LocationServices
-dontwarn com.google.android.gms.tasks.OnFailureListener
-dontwarn com.google.android.gms.tasks.OnSuccessListener
-dontwarn com.google.android.gms.tasks.Task
-dontwarn com.google.android.gms.tasks.TaskCompletionSource
-dontwarn com.google.errorprone.BugPattern
-dontwarn com.google.errorprone.bugpatterns.BugChecker$AnnotationTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$IdentifierTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$MemberSelectTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$MethodInvocationTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$MethodTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$SynchronizedTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker$VariableTreeMatcher
-dontwarn com.google.errorprone.bugpatterns.BugChecker
-dontwarn com.google.errorprone.matchers.Matcher
-dontwarn com.google.errorprone.matchers.Matchers
-dontwarn com.google.errorprone.matchers.method.MethodMatchers$InstanceMethodMatcher
-dontwarn com.google.errorprone.matchers.method.MethodMatchers$MethodClassMatcher
-dontwarn com.google.errorprone.matchers.method.MethodMatchers$MethodNameMatcher
-dontwarn com.google.errorprone.matchers.method.MethodMatchers$ParameterMatcher
-dontwarn com.google.errorprone.predicates.TypePredicate
-dontwarn com.google.errorprone.predicates.TypePredicates
-dontwarn com.google.errorprone.suppliers.Supplier
-dontwarn com.google.errorprone.suppliers.Suppliers
-dontwarn com.sun.source.util.SimpleTreeVisitor
-dontwarn com.uber.nullaway.LibraryModels
-dontwarn java.awt.Color
-dontwarn java.awt.Font
-dontwarn java.awt.Point
-dontwarn java.awt.Rectangle
-dontwarn javax.money.CurrencyUnit
-dontwarn javax.money.Monetary
-dontwarn javax.ws.rs.Consumes
-dontwarn javax.ws.rs.Produces
-dontwarn javax.ws.rs.core.Response
-dontwarn javax.ws.rs.core.StreamingOutput
-dontwarn javax.ws.rs.ext.MessageBodyReader
-dontwarn javax.ws.rs.ext.MessageBodyWriter
-dontwarn javax.ws.rs.ext.Provider
-dontwarn org.chromium.net.impl.CronetLogger$CronetEngineBuilderInitializedInfo$Author
-dontwarn org.chromium.net.impl.CronetLogger$CronetEngineBuilderInitializedInfo
-dontwarn org.chromium.net.impl.CronetLogger$CronetSource
-dontwarn org.chromium.net.impl.CronetLogger$CronetVersion
-dontwarn org.chromium.net.impl.CronetLogger
-dontwarn org.chromium.net.impl.CronetLoggerFactory
-dontwarn org.glassfish.jersey.internal.spi.AutoDiscoverable
-dontwarn org.javamoney.moneta.Money
-dontwarn org.joda.time.DateTime
-dontwarn org.joda.time.DateTimeZone
-dontwarn org.joda.time.Duration
-dontwarn org.joda.time.Instant
-dontwarn org.joda.time.LocalDate
-dontwarn org.joda.time.LocalDateTime
-dontwarn org.joda.time.LocalTime
-dontwarn org.joda.time.Period
-dontwarn org.joda.time.ReadablePartial
-dontwarn org.joda.time.format.DateTimeFormat
-dontwarn org.joda.time.format.DateTimeFormatter
-dontwarn springfox.documentation.spring.web.json.Json

## ==== SPEC-151: WebView JS 结果 DTO 的 GSON 反射保护 ====
## 这些 DTO 由 WebView JS 返回的 JSON 经 GSON 按字段名反射解析（无 @SerializedName 时），
## R8 混淆重命名字段会导致 JSON 键失配、解析全 null（1.5.3/1.5.4「未能提取正文」归根因）。
## 注解化（@SerializedName）是主修复；此规则兜底覆盖后续新增的同类 DTO。
-keepclassmembernames class io.legado.app.browser.reader.** { <fields>; }
-keepclassmembernames class io.legado.app.ui.browser.BrowserContextMenuPayload { <fields>; }

## ==== SPEC-153: Bromium AwSettings 反射兜底 ====
## 主修复是反射改直调（AwContentsHostView/BrowserPrivacyControls）；若后续再引入
## 按名反射调用 AwSettings 成员，R8 重命名会让 getMethod 静默失败（网页背景
## Force Dark 失效归根因），此规则保持成员名与未删除。
-keepclassmembers class org.chromium.android_webview.AwSettings {
    public void setForceDarkMode(int);
    public void setForceDarkBehavior(int);
    public void setAlgorithmicDarkeningAllowed(boolean);
    public void setSafeBrowsingEnabled(boolean);
}
