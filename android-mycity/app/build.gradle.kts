plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sheharsaathi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sheharsaathi.app"
        minSdk = 24
        targetSdk = 36
        // Each GitHub build gets a higher number so phones accept it as an update.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1)
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        abortOnError = false
        textReport = true
        textOutput = file("build/reports/lint.txt")
    }

    // One fixed key for test builds, so each new build installs over the previous one on a phone.
    // Only for testing; a Play Store release needs its own private key kept out of the repo.
    signingConfigs {
        create("testing") {
            storeFile = file("testing.keystore")
            storePassword = "sheharsaathi-test"
            keyAlias = "testing"
            keyPassword = "sheharsaathi-test"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("testing")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("testing")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.osmdroid.android)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.browser:browser:1.8.0") // opens news stories in an in-app browser tab
    debugImplementation(libs.androidx.ui.tooling)

    // Tests: JVM unit tests (app/src/test) and the on-device walkthrough (app/src/androidTest)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
