plugins {
    id("kmp.library")
    id("maven.publish")
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":api-call-error"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
        }

        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.coroutines.test)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }

        jvmTest.dependencies {
            implementation(libs.logback.classic)
        }
    }
}
