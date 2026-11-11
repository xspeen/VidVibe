import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.vidvibe.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "VidVibe"
            packageVersion = "2.5.0"
            description = "VidVibe video downloader for Windows"
            vendor = "VidVibe"
            copyright = "MIT License"

            windows {
                menuGroup = "VidVibe"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                upgradeUuid = "8f1c2b76-3a1e-4a8e-9a52-4a0d3b7c6e11"
            }
        }
    }
}
