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

includeBuild("libs/KMPLiquidGlass") {
    dependencySubstitution {
        substitute(module("io.github.kashif-mehmood-km:backdrop")).using(project(":backdrop"))
    }
}
