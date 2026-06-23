import java.net.URL

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.activitylauncher.plugin.shizuku"
    compileSdk = 37

    defaultConfig {
        applicationId = "net.activitylauncher.plugin.shizuku"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "net.activitylauncher.plugin.shizuku"
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        aidl = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    useLibrary("android.test.runner")
    useLibrary("android.test.base")
    useLibrary("android.test.mock")
}

val shizukuApkUrl = "https://github.com/RikkaApps/Shizuku/releases/download/v13.6.0/shizuku-v13.6.0.r1086.2650830c-release.apk"
val shizukuDir = layout.buildDirectory.dir("shizuku")
val shizukuApkFile = shizukuDir.map { it.file("shizuku.apk") }

val downloadShizuku = tasks.register("downloadShizuku") {
    description = "Downloads the Shizuku APK for integration tests"
    val apkFile = shizukuApkFile
    val url = shizukuApkUrl
    outputs.file(apkFile)
    doLast {
        val destination = apkFile.get().asFile
        if (!destination.exists()) {
            println("Downloading Shizuku from $url...")
            destination.parentFile.mkdirs()
            URL(url).openStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            println("Downloaded to ${destination.absolutePath}")
        }
    }
}

val installShizuku = tasks.register<Exec>("installShizuku") {
    description = "Installs Shizuku APK on the connected device"
    dependsOn(downloadShizuku)
    commandLine("adb", "install", "-r", "-t", shizukuApkFile.get().asFile.absolutePath)
    doFirst {
        println("Installing Shizuku on device...")
    }
}

tasks.matching { it.name == "connectedDebugAndroidTest" }.configureEach {
    dependsOn(installShizuku)
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}