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
    plugins {
        id("org.jetbrains.kotlin.multiplatform").version(extra["kotlin.version"] as String).apply(false)
        id("org.jetbrains.compose").version(extra["compose.version"] as String).apply(false)
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        ivy {
            name = "Node.js Distributions"
            url = uri("https://nodejs.org/dist")
            patternLayout {
                artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("org.nodejs", "node")
            }
        }
    }
}

rootProject.name = "KMPLiquidGlass"
// 零语只内嵌了 :backdrop 这一个模块（其余 catalog/app/artworks 等已从仓库移除，
// 见根目录 README 的「第三方组件」说明），因此这里只 include 它。
include(":backdrop")
