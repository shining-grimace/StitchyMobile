
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
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
