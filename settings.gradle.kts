pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven {
            url = uri("https://jitpack.io")
            content {
                includeGroupByRegex("com\\.github.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "browsesth"

val legadoRoot = file("upstream/legado-with-MD3")

include(":app")
project(":app").projectDir = file("$legadoRoot/app")

include(":modules:book")
project(":modules:book").projectDir = file("$legadoRoot/modules/book")

include(":modules:rhino")
project(":modules:rhino").projectDir = file("$legadoRoot/modules/rhino")
