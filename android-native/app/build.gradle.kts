plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

// 正式 release 打包要求签名配置；本地安装可通过显式参数复用 debug key。
// 不默认回退签名，避免把本地测试包误当成正式发布包。
//
// 只拦真正产出已签名产物的任务。早先按 "任务名含 Release" 判定，结果连
// compileReleaseKotlin / lintRelease 这类与签名无关的任务也被挡下，
// 连 release 代码能否编译都验证不了。
val signedArtifactTasks = setOf(
    "assembleRelease", "bundleRelease", "packageRelease", "installRelease",
    "assembleAndroidTestRelease", "publishReleaseBundle",
    // 聚合任务：`./gradlew build` 内部会跑到 assembleRelease，但任务名里看不到它
    "build",
)
val needsKeystore = gradle.startParameter.taskNames.any { taskPath ->
    taskPath.substringAfterLast(':') in signedArtifactTasks
}
val allowDebugSigning = providers.gradleProperty("allowDebugSigning").orNull == "true"
if (needsKeystore && !keystorePropertiesFile.exists() && !allowDebugSigning) {
    throw GradleException(
        "缺少 keystore.properties，无法执行 release 打包。\n" +
            "请复制 keystore.properties.example 为 keystore.properties 并填入正式签名信息。"
    )
}

android {
    namespace = "com.example.randomgallery.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.randomgallery.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "DEFAULT_BASE_URL", "\"https://example.invalid/\"")
        buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")
        // release：只允许内网/回环走明文，公网必须 HTTPS。这条策略由 CleartextPolicy 在
        // 校验服务地址时执行 —— NSC 无法表达网段，见该类的说明。
        buildConfigField("boolean", "PERMIT_PUBLIC_CLEARTEXT", "false")
    }

    // 只产出 arm64-v8a 单包。原先 include(arm64-v8a, armeabi-v7a) + isUniversalApk=true
    // 会把 x86 / x86_64 / v7a 的全套 avif-coder 静态库一起打进 universal 包（63MB）。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
            buildConfigField("String", "DEFAULT_BASE_URL", "\"http://10.0.2.2:8086/\"")
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
            // debug：连任意调试机，公网明文也放行
            buildConfigField("boolean", "PERMIT_PUBLIC_CLEARTEXT", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else if (allowDebugSigning) {
                signingConfigs.getByName("debug")
            } else {
                // 允许无签名的编译 / lint，正式打包由顶部检查拦截。
                null
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = true
        abortOnError = false
    }

    testOptions {
        unitTests {
            // ViewModel 用 android.util.Log 记录失败原因，而纯 JVM 单测里 android.jar 是桩：
            // 默认抛 "not mocked" 会让协程在 Log 那一行中断，后面的状态收敛（清 loading 等）
            // 全部不再执行，测试便只能观察到被截断的行为。改为返回默认值。
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// 不配置 stabilityConfigurationFiles：实测（app/build/compose-reports/ 编译器指标）
// 加与不加的可跳过 composable 数完全一致，composables.txt 逐字节相同。Kotlin 2.2 默认开启
// strong skipping，87 个 restartable composable 全部已 skippable；剩余 21 个不稳定参数里
// 10 个是 viewModel（编译器有意判为不稳定），11 个是裸 List<T> 参数——List 的"不稳定"是
// 编译器内置判定，stabilityConfigurationFiles 改不了它。详见提交说明。

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    // material：XML 主题 Theme.Material3.Light.NoActionBar 依赖此库（AppCompatActivity 主题）
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.coil)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // libheif 软解兜底（系统 HEIC 解码失败时使用），见 HeifSystemFirstDecoder
    implementation(libs.avif.coder)

    // ExoPlayer via Media3 — 用于播放随机动图（实为视频文件）
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // --- Jetpack Compose（迁移用，先与现有 View 体系共存）---
    // BOM 统一管理 Compose 各库版本；如需更新特性可整体上调 libs.versions.toml 里的 composeBom。
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.material3)
    // REQ-06：大屏 / 横屏自适应判定的标准依据（版本由 Compose BOM 管理）
    implementation(libs.androidx.material3.window.size)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 依赖注入：Hilt 编译期生成依赖图，替代手写 Factory 与 AppContainer 手工装配
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // hiltViewModel() 的新家：androidx.hilt.navigation.compose 的旧版本已标记 @Deprecated
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Baseline Profile 运行时（Profiles 由 src/main/baseline-prof.txt 提供）
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    // REQ-09：viewModelScope 依赖 Dispatchers.Main，纯 JVM 测试需要 setMain
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    // REQ-09：Compose 交互测试（仅编译校验，执行需真机 / 模拟器）
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.uiautomator)

    // REQ-11：类型安全导航路由的 @Serializable 运行时
    implementation(libs.kotlinx.serialization.json)
}
