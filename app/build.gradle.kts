import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shininggrimace.stitchy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shininggrimace.stitchy"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.0.1"
    }
    signingConfigs {
        create("release") {
            val keystoreProperties = Properties().apply {
                load(FileInputStream("/home/thomas/Android/keystore/grimace_keystore.properties.txt"))
            }
            storeFile = file("/home/thomas/Android/keystore/grimace_keystore.jks")
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".test"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.material:material:1.13.0-alpha10")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.7")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("io.coil-kt.coil3:coil:3.1.0")
}

tasks.register<Exec>("compileRust") {
    commandLine("cargo", "build", "--target", "aarch64-linux-android", "--release", "--manifest-path", "../rust/Cargo.toml")
    commandLine("cargo", "build", "--target", "x86_64-linux-android", "--release", "--manifest-path", "../rust/Cargo.toml")

    doLast {
        project.copy {
            from("../rust/target/aarch64-linux-android/release/librust.so")
            into("src/main/jniLibs/arm64-v8a")
        }
        project.copy {
            from("../rust/target/x86_64-linux-android/release/librust.so")
            into("src/main/jniLibs/x86_64")
        }
        println("Rust compiler completed")
    }
}

tasks.named("preBuild").configure {
    dependsOn("compileRust")
}
