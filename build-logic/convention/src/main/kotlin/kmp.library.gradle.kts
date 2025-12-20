@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()

    androidLibrary {
        namespace = project.findProperty("MODULE_NAMESPACE") as String
        compileSdk = (project.findProperty("ANDROID_COMPILE_SDK") as String).toInt()
        minSdk = (project.findProperty("ANDROID_MIN_SDK") as String).toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Linux targets
    linuxX64()
    linuxArm64()

    // JS targets
    js {
        browser()
    }

    wasmJs {
        browser()
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

