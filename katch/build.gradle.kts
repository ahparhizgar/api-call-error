plugins {
    id("kmp.library")
    id("maven.publish")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
        }
    }
}
