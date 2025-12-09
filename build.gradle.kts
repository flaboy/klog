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

    // Desktop Native targets
    macosArm64()
    macosX64()
    linuxX64()
    mingwX64()

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
        val androidMain by getting {
            dependsOn(commonMain)
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")
            }
        }
        val jvmMain by getting {
            dependsOn(commonMain)
        }
        
        val jvmTest by getting {
            dependencies {
                implementation("com.squareup.okio:okio-fakefilesystem:3.9.0")
            }
        }
        // iOS targets share common implementation
        val iosMain by creating {
            dependsOn(commonMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        
        // Native targets share common implementation
        val nativeMain by creating {
            dependsOn(commonMain)
        }
        
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
        
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        
        val mingwX64Main by getting {
            dependsOn(nativeMain)
        }
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

