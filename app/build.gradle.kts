// app/build.gradle.kts

// This plugins block MUST be at the very top of the file
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    // This plugin applies the kapt annotation processor configuration
    alias(libs.plugins.kotlinKapt)
    // The parcelize plugin is needed for data classes that are Parcelable
    alias(libs.plugins.kotlinParcelize)
    // The Hilt plugin for dependency injection
    alias(libs.plugins.hiltAndroid)
    // The Google Services plugin for Firebase integration
    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.expensestracker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.expensestracker"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    // Enable View Binding for your UI
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Use the bundles defined in the TOML file for clean dependency management
    implementation(libs.bundles.androidx.ui)
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.room)
    implementation(libs.bundles.test)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Firebase BOM to manage versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)

    // MPAndroidChart
    implementation(libs.mpandroidchart)

    // Room compiler with kapt
    kapt(libs.androidx.room.compiler)
    implementation("com.google.firebase:firebase-auth-ktx")

}
