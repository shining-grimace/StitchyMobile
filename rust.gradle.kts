
tasks.register<Exec>("cargoBuildAarch64") {
    outputs.upToDateWhen { false }
    commandLine(
        "cargo",
        "build",
        "--target",
        "aarch64-linux-android",
        "--release",
        "--manifest-path",
        "rust/Cargo.toml"
    )
}

tasks.register<Exec>("cargoBuildX86_64") {
    outputs.upToDateWhen { false }
    commandLine(
        "cargo",
        "build",
        "--target",
        "x86_64-linux-android",
        "--release",
        "--manifest-path",
        "rust/Cargo.toml"
    )
}

tasks.register("copyAarch64Lib") {
    dependsOn("cargoBuildAarch64")
    doLast {
        project.copy {
            from("rust/target/aarch64-linux-android/release/librust.so")
            into("app/src/main/jniLibs/arm64-v8a")
        }
    }
    outputs.upToDateWhen { false }
}

tasks.register("copyX86_64Lib") {
    dependsOn("cargoBuildX86_64")
    doLast {
        project.copy {
            from("rust/target/x86_64-linux-android/release/librust.so")
            into("app/src/main/jniLibs/x86_64")
        }
    }
    outputs.upToDateWhen { false }
}

tasks.register("rustBuild") {
    dependsOn("copyAarch64Lib", "copyX86_64Lib")
    doLast {
        file("app/src/main/jniLibs").setLastModified(System.currentTimeMillis())
    }
}
