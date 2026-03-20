plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.example.grocerycompare"
    compileSdk = 35

    // Override via gradle.properties or CI env: BACKEND_URL=https://your-app.onrender.com
    val backendUrl: String = (project.findProperty("BACKEND_URL") as String?)
        ?: System.getenv("BACKEND_URL")
        ?: "http://10.0.2.2:8000"  // Android emulator → host machine localhost

    // Release signing — only configured when keystore properties are provided.
    // Falls back to debug signing so the app can always be run in Android Studio.
    val keystorePath     = (project.findProperty("KEYSTORE_PATH")     as String?) ?: System.getenv("KEYSTORE_PATH")
    val keystorePassword = (project.findProperty("KEYSTORE_PASSWORD") as String?) ?: System.getenv("KEYSTORE_PASSWORD")
    val keyAlias         = (project.findProperty("KEY_ALIAS")         as String?) ?: System.getenv("KEY_ALIAS")
    val keyPassword      = (project.findProperty("KEY_PASSWORD")      as String?) ?: System.getenv("KEY_PASSWORD")
    val hasKeystoreConfig = keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null

    if (hasKeystoreConfig) {
        signingConfigs {
            create("release") {
                storeFile          = file(keystorePath!!)
                storePassword      = keystorePassword
                this.keyAlias      = keyAlias!!
                this.keyPassword   = keyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.grocerycompare"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BACKEND_URL", "\"$backendUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Use release signing if keystore is configured, otherwise fall back to debug
            signingConfig = if (hasKeystoreConfig)
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "COPYRIGHT.txt"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Suppress version-update warnings for deps that require a higher AGP than we currently use
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi", "ObsoleteLintCustomCheck")
        warningsAsErrors = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.timber)
    implementation(libs.slf4j.simple)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
