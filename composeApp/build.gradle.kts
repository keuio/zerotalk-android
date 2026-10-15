import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    // Baseline Profile 的消费侧：让本模块能接收 :baselineprofile 采集到的启动期 profile
    alias(libs.plugins.androidx.baselineprofile)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // 平台桥接使用 expect/actual object（系统通知），该特性仍标记为 Beta，屏蔽其告警
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.kashif-mehmood-km:backdrop")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
            // 生命周期感知的状态收集：collectAsStateWithLifecycle 在界面不可见时
            // 停止收集，避免后台持续重组与耗电（本项目 UI 层原先全部用 collectAsState）。
            implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.9.6")
            implementation("com.squareup.okhttp3:okhttp:4.12.0")
            implementation("com.google.code.gson:gson:2.11.0")
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.foundation)
            // 语音通话：WebRTC Android SDK（音频-only，与网页端互通；含 x86_64 so，模拟器可跑）
            implementation("io.github.webrtc-sdk:android:144.7559.15")
            // Baseline Profile 的运行时装填器：AGP 与 baselineprofile 插件都不会自动引入，
            // 缺了它 Android 12 以下设备不会应用 profile（且不会报错，只是静默无效）
            implementation(libs.androidx.profileinstaller)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
        }
    }
}

// 发布签名：从仓库根目录的 keystore.properties 读取（该文件与密钥库均不入库）。
// 文件不存在时保持未签名，保证任何人 clone 后仍能正常构建。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
val hasReleaseSigning = !keystoreProps.getProperty("storeFile").isNullOrBlank()

android {
    namespace = "top.lanxint.zerotalk"
    compileSdk = 36

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            // R8 代码压缩 + 资源压缩。
            // 未开启时 release 包会带上全部未使用字节码（实测 dex 48.5MB / APK 60MB）。
            // 规则与每条规则的依据见 proguard-rules.pro。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ABI 过滤：只打包真机需要的 arm64-v8a / armeabi-v7a。
            // x86 / x86_64 的 WebRTC so 仅供模拟器使用，在发布包里白占 27MB。
            // 只在 release 生效，debug 构建保留全部 ABI，模拟器照常可跑。
            ndk {
                abiFilters.clear()
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }

        // Baseline Profile 采集专用变体。
        //
        // 插件默认会基于 release 自动创建它，但 initWith(release) 会连带继承上面
        // 的 abiFilters（只含 arm），于是采集只能跑在走 ARM 转译的 x86_64 模拟器上
        // —— 实测 Macrobenchmark 的启动完成检测会超时失败。
        // 这里手动创建并放开 x86_64：插件发现该 buildType 已存在时只覆盖
        // isDebuggable / isMinifyEnabled / isProfileable 等项，不会动 ndk 配置。
        // 发布用的 release 仍然只含 arm，包体不受影响。
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            ndk {
                abiFilters.clear()
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            }
        }
    }

    defaultConfig {
        applicationId = "top.lanxint.zerotalk"
        minSdk = 23
        targetSdk = 36
        versionCode = 13
        versionName = "1.8.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Compose 编译器报告：输出每个 composable 的 restartable / skippable 情况与参数稳定性。
// Windows 下直接开启会导致 JsonBuilderKt.write 生成含非法字符路径报 "Invalid file path" 异常。
// 默认关闭，仅在显式传入 -PcomposeCompilerMetrics=true 时启用。
if (project.findProperty("composeCompilerMetrics") == "true") {
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        metricsDestination = layout.buildDirectory.dir("compose_compiler")
    }
}

baselineProfile {
    // 采集结果写回源码树并提交，避免每次构建都重新采集
    saveInSrc = true
    // KMP 模块：合并进 androidMain，最终落在
    // composeApp/src/androidMain/generated/baselineProfiles/
    mergeIntoMain = true
}

dependencies {
    // 接收 :baselineprofile 采集到的启动期 profile
    baselineProfile(project(":baselineprofile"))
}
