pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ZeroTalk"
include(":composeApp")
// Baseline Profile 生成模块（com.android.test）：只在开发机/CI 上跑，不进发布包
include(":baselineprofile")

includeBuild("libs/KMPLiquidGlass") {
    dependencySubstitution {
        substitute(module("io.github.kashif-mehmood-km:backdrop")).using(project(":backdrop"))
    }
}
