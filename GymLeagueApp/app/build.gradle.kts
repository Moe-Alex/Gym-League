plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.gymleague.app"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.gymleague.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            storeFile =
                rootProject.file("debug.keystore")

            storePassword =
                "android"

            keyAlias =
                "androiddebugkey"

            keyPassword =
                "android"
        }
    }

    buildTypes {

        debug {
            signingConfig =
                signingConfigs.getByName("debug")
        }

        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    // RecyclerView for Exercise Library
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Android biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    implementation(
        "com.squareup.retrofit2:retrofit:2.11.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:2.11.0"
    )

    implementation(
        "com.squareup.okhttp3:logging-interceptor:4.12.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0"
    )

    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.19.0"
        )
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    // Firebase Cloud Messaging (push notifications)
    implementation(
        "com.google.firebase:firebase-messaging"
    )

    implementation(
        "androidx.credentials:credentials:1.3.0"
    )

    implementation(
        "androidx.credentials:credentials-play-services-auth:1.3.0"
    )

    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )
    // Room (offline database). KSP, not kapt.
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // WorkManager (background sync when the internet comes back)
    implementation("androidx.work:work-runtime:2.11.2")
    testImplementation(libs.junit)

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )
}