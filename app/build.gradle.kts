import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.chaquo.python")
}

val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.isai.trader"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.isai.trader"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    chaquopy {
        defaultConfig {
            version = "3.12"
            pip {
                options("--find-links", "/workspaces/Isai/app/wheels")
                install("wheels/jiter-0.16.0-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/jiter-0.16.0-cp312-cp312-android_24_x86_64.whl")
                install("wheels/msgpack-1.1.2-py3-none-any.whl")
                install("wheels/orjson-3.12.0-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/orjson-3.12.0-cp312-cp312-android_24_x86_64.whl")
                install("wheels/ormsgpack-1.12.2-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/ormsgpack-1.12.2-cp312-cp312-android_24_x86_64.whl")
                install("wheels/pydantic_core-2.46.4-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/pydantic_core-2.46.4-cp312-cp312-android_24_x86_64.whl")
                install("wheels/tiktoken-0.14.0-py3-none-android_24_arm64_v8a.whl")
                install("wheels/tiktoken-0.14.0-py3-none-android_24_x86_64.whl")
                install("wheels/uuid_utils-0.17.0-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/uuid_utils-0.17.0-cp312-cp312-android_24_x86_64.whl")
                install("wheels/xxhash-3.8.1-cp312-cp312-android_24_arm64_v8a.whl")
                install("wheels/xxhash-3.8.1-cp312-cp312-android_24_x86_64.whl")
                install("-r", "requirements.txt")
            }
        }
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    signingConfigs {
        create("release") {
            if (keystoreProperties.isNotEmpty()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
