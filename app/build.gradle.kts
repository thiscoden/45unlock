import com.android.build.api.artifact.SingleArtifact
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.unlock45.tv"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.unlock45.tv"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Distribution is ADB-only (no Play Store). Signing with the local debug key keeps
            // `adb install -r` updates working without a separate release keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // The target platform is Android TV 14 (API 34).
        disable += "OldTargetApi"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // androidx.graphics:graphics-path (pulled in by Compose) only needs its native library
            // for path iteration below API 34; with minSdk 34 it uses the platform PathIterator.
            excludes += "**/libandroidx.graphics.path.so"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// The release build ships as a single universal `45unlock.apk` in build/dist.
// It must stay free of native libraries, otherwise it would no longer be universal.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        val apkDir = variant.artifacts.get(SingleArtifact.APK)
        val loader = variant.artifacts.getBuiltArtifactsLoader()
        val distDir = layout.buildDirectory.dir("dist")
        tasks.register("distReleaseApk") {
            description = "Copies the release APK to build/dist/45unlock.apk and checks it is universal."
            inputs.dir(apkDir)
            outputs.dir(distDir)
            doLast {
                val built = loader.load(apkDir.get()) ?: error("No release APK built")
                val apk = built.elements.singleOrNull()?.outputFile?.let(::File)
                    ?: error("Expected exactly one release APK, found ${built.elements.size}")
                ZipFile(apk).use { zip ->
                    val nativeLibs = zip.entries().asSequence().filter { it.name.startsWith("lib/") }.map { it.name }.toList()
                    check(nativeLibs.isEmpty()) { "Release APK must not contain native libraries: $nativeLibs" }
                }
                val target = distDir.get().file("45unlock.apk").asFile
                apk.copyTo(target, overwrite = true)
                logger.lifecycle("Release APK: ${target.absolutePath}")
            }
        }
    }
}

tasks.matching { it.name == "assembleRelease" }.configureEach { finalizedBy("distReleaseApk") }
