pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // 唯一保留内联版本号的地方：settings 脚本的 plugins {} 块在版本目录加载之前求值，
    // `alias(libs.plugins...)` 会报 `Unresolved reference 'libs'`（Gradle 9.4.1 实测）。
    // 其余依赖与插件版本全部取自 gradle/libs.versions.toml。
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RandomGalleryNative"
include(":app")
