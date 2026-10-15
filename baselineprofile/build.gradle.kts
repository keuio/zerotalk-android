import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Baseline Profile 生成模块。
//
// 这是一个 com.android.test 模块：它不会打进发布包，只在开发机 / CI 上运行，
// 驱动 :composeApp 的 nonMinifiedRelease 变体跑一遍启动路径，把热点方法记录成
// Baseline Profile，再由 :composeApp 侧的 consumer 插件合并进构建。
//
// KMP 模块的落地路径是 src/androidMain/generated/baselineProfiles/
// （由 :composeApp 的 baselineProfile { mergeIntoMain = true } 决定）。
//
// 生成命令：
//   ./gradlew :baselineprofile:connectedNonMinifiedReleaseAndroidTest   # 设备上采集
//   ./gradlew :composeApp:generateBaselineProfile                        # 采集并合并
// （需要一台已连接且能安装 :composeApp nonMinifiedRelease 包的设备/模拟器）
plugins {
    // 不带版本：AGP / Kotlin 已由 :composeApp 放到根 classpath 上，
    // 再声明版本会触发 "already on the classpath with an unknown version" 冲突
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "top.lanxint.zerotalk.baselineprofile"
    compileSdk = 36

    defaultConfig {
        // baseline profile 采集依赖 Macrobenchmark，最低 API 23 即可，但
        // profileinstaller 的 shell 采集路径在 28+ 更稳定
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 指向被测应用模块
    targetProjectPath = ":composeApp"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // 与 :composeApp 对齐到 Java 17，否则 Java 任务默认 11 而 Kotlin 任务用 21，
    // Gradle 会以 "Inconsistent JVM Target Compatibility" 中断编译
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

baselineProfile {
    // 使用当前连接的设备，不启动 GMD 托管设备。
    // 其余（profile 写回源码树 composeApp/src/release/baseline-prof.txt）走插件默认行为。
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
