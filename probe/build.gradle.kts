import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val versionPropsFile = file("version.properties")

fun tenthsToName(tenths: Int): String = "${tenths / 10}.${tenths % 10}"

fun loadVersionProps(): Properties = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}

fun saveVersionProps(props: Properties, comment: String) {
    versionPropsFile.outputStream().use { props.store(it, comment) }
}

// Operator rule: every app assemble* build → versionName +0.1, versionCode +1.
// Bump during configuration when an assemble task is requested, so THIS APK carries the new version.
val requestingAssemble = gradle.startParameter.taskNames.any {
    val n = it.substringAfterLast(':')
    n.startsWith("assemble") && n != "assemble"
}
val versionProps = loadVersionProps()
if (requestingAssemble) {
    val nextTenths = versionProps.getProperty("VERSION_TENTHS", "1").toInt() + 1
    val nextCode = versionProps.getProperty("VERSION_CODE", "1").toInt() + 1
    versionProps.setProperty("VERSION_TENTHS", nextTenths.toString())
    versionProps.setProperty("VERSION_CODE", nextCode.toString())
    saveVersionProps(versionProps, "Bumped by assemble (+0.1)")
    logger.lifecycle("Browsesth version -> ${tenthsToName(nextTenths)} (code $nextCode)")
}

val versionTenths = versionProps.getProperty("VERSION_TENTHS", "1").toInt()
val versionCodeValue = versionProps.getProperty("VERSION_CODE", "1").toInt()

android {
    namespace = "com.browsesth.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.browsesth.app"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeValue
        versionName = tenthsToName(versionTenths)
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }

    androidResources {
        noCompress += "bin"
        noCompress += "dat"
        noCompress += "pak"
    }
}

dependencies {
    implementation(files("libs/aloha-bromium-localbuild.aar"))
    // Aloha/Chromium media stack needs Guava (AudioManagerAndroid → ImmutableMap).
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
