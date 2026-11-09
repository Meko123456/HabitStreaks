plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.github.meko123456.habitstreaks"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.meko123456.habitstreaks"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Code shrinking alone leaves every drawable, string and style the shrunk code no
            // longer references sitting in the APK.
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Room writes each version's schema here so a future migration can be written against the real
// previous schema and reviewed in the diff, rather than reconstructed from memory.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    implementation(libs.heatmap.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    // Compose UI tests on a device: what TalkBack is told, which a JVM test can't see.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    // The Compose test library brings Espresso 3.5.0, which looks up InputManager.getInstance by
    // reflection the first time a test waits for idle. Android 17 removed it, so every Compose
    // test failed there with NoSuchMethodException. 3.7.0 asks the Context for the InputManager.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.compose.ui.test.manifest)
}
