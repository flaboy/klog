plugins {
    kotlin("multiplatform") version "2.2.21"
    id("com.android.library") version "8.7.3"
}

group = "com.flaboy"
version = "1.0.0-SNAPSHOT"

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
            val commonMain by getting {
                dependencies {
                    implementation("com.squareup.okio:okio:3.9.0")
                    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                }
            }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")
            }
        }
        val androidMain by getting
        val androidUnitTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
    }
}

android {
    namespace = "com.flaboy.klog"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

