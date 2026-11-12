import java.util.Properties

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
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        // Rive ships native code per CPU; phones only need ARM. (x86 emulators are left out.)
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }
    // Compress the native libraries in the side-loaded APK; they are unpacked at install.
    packaging { jniLibs { useLegacyPackaging = true } }

    // The upload key lives outside the repo: keystore.properties (storeFile, storePassword, keyAlias,
    // keyPassword) next to settings.gradle.kts. Without it, release builds use the debug key.
    val keys = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f -> Properties().apply { f.inputStream().use(::load) } }
    signingConfigs {
        if (keys != null) create("upload") {
            storeFile = rootProject.file(keys.getProperty("storeFile"))
            storePassword = keys.getProperty("storePassword")
            keyAlias = keys.getProperty("keyAlias")
            keyPassword = keys.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            // Shrinking (R8) only when asked for with -Pshrink, until a shrunk build has been tried on a phone.
            isMinifyEnabled = project.hasProperty("shrink")
            isShrinkResources = project.hasProperty("shrink")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }

    // sideload: the APK testers install by hand. play: the bundle for Google Play (no direct
    // battery-optimisation request, which Play allows only for a few kinds of apps).
    flavorDimensions += "store"
    productFlavors {
        create("sideload") { dimension = "store"; buildConfigField("boolean", "PLAY", "false") }
        create("play") { dimension = "store"; buildConfigField("boolean", "PLAY", "true") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
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
    implementation(libs.compose.icons)
    implementation(libs.health.connect)
    implementation(libs.rive)
    // QR code on the shareable garden card.
    implementation("com.google.zxing:core:3.5.3")
    // Sign in with Google (Credential Manager); the ID token goes to Supabase Auth.
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    // In-app review prompt (Play build only).
    "playImplementation"("com.google.android.play:review-ktx:2.0.2")
    // The review library brings an old Fragment; activity-result calls need 1.3+.
    "playImplementation"("androidx.fragment:fragment:1.8.5")
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
