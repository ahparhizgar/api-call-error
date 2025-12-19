@file:OptIn(ExperimentalWasmDsl::class)

import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlinx.serialization)
//    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.ahparhizgar.apicallerror"
version = "0.1.0"

kotlin {
    jvm()
    androidLibrary {
        namespace = "com.ahparhizgar.apicallerror"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_11
                )
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    linuxArm64()
    js {
        browser()
    }
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":api-call-error"))
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
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

//mavenPublishing {
//    publishToMavenCentral()
//
//    signAllPublications()
//
//    coordinates(group.toString(), "library", version.toString())
//
//    pom {
//        name = "My library"
//        description = "A library."
//        inceptionYear = "2024"
//        url = "https://github.com/kotlin/multiplatform-library-template/"
//        licenses {
//            license {
//                name = "XXX"
//                url = "YYY"
//                distribution = "ZZZ"
//            }
//        }
//        developers {
//            developer {
//                id = "XXX"
//                name = "YYY"
//                url = "ZZZ"
//            }
//        }
//        scm {
//            url = "XXX"
//            connection = "YYY"
//            developerConnection = "ZZZ"
//        }
//    }
//}
