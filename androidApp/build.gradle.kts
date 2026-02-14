import io.kotzilla.gradle.ext.KotzillaKeyGeneration

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotzilla)
}

android {
    namespace = "io.github.alexmaryin.followmymus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "io.github.alexmaryin.followmymus"
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        versionCode = libs.versions.android.versionCode.get().toInt()
        versionName = libs.versions.packageVersion.get()
    }

    kotzilla {
        versionName = libs.versions.packageVersion.get()
        composeInstrumentation = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(project(":composeApp"))
}