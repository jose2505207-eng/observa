plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.observa.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.observa.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            // Extract native libs at install so the Hexagon DSP can load the QNN v79 skel from a
            // real filesystem path (fastRPC cannot load it from inside the compressed APK).
            useLegacyPackaging = true
        }
    }
    testOptions {
        unitTests {
            // Let android.util.Log (and other stubs) return defaults instead of throwing in JVM tests.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(files("libs/executorch.aar"))
    // ExecuTorch's Java Module loads its native libs via Facebook SoLoader/fbjni; the AAR is a bare
    // file dependency so these transitive runtime deps must be declared explicitly (versions pinned
    // to match executorch/extension/android/executorch_android/build.gradle).
    implementation("com.facebook.fbjni:fbjni:0.7.0")
    implementation("com.facebook.soloader:nativeloader:0.10.5")
    implementation(libs.mlkit.text.recognition)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}