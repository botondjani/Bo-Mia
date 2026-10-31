plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.tend"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.tend"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // Rive ships native code per CPU; phones only need ARM. (x86 emulators are left out.)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    // Compress the native libraries in the side-loaded APK; they are unpacked at install.
    packaging { jniLibs { useLegacyPackaging = true } }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Test APK: signed with the debug key so it can be side-loaded.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.health.connect)
    implementation(libs.rive)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
