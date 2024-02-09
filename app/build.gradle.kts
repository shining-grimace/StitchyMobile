
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shininggrimace.stitchy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shininggrimace.stitchy"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("io.coil-kt:coil:1.4.0")
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
