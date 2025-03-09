
plugins {
    id("com.android.application") version "8.9.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
}

apply(from = rootProject.file("rust.gradle.kts"))

subprojects {
    afterEvaluate {
        if (name == "app") {
            tasks.named("preBuild") {
                dependsOn(":rustBuild")
            }
        }
    }
}
