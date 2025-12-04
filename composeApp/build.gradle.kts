import com.codingfeline.buildkonfig.compiler.FieldSpec
import io.kotzilla.gradle.ext.KotzillaKeyGeneration
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.internal.utils.getLocalProperty
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildKonfig)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotzilla)
}

kotlin {

    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xcontext-parameters")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(libs.decompose)
            export(libs.decompose.essenity)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.preview)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)

            // Koin DI Android
            implementation(libs.koin.android)
            // KTOR client
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            // Camera & ML Kit to recognize QR
            implementation(libs.camera2)
            implementation(libs.camera.lifecycle)
            implementation(libs.camera.view)
            implementation(libs.mlkit.barcode)
            // Kotzilla Analytics
            implementation(libs.kotzilla.sdk.compose)
        }

        iosMain.dependencies {
            // KTOR client
            implementation(libs.ktor.client.darwin)
            // Kotzilla Analytics
            implementation(libs.kotzilla.sdk.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // KTOR client
            implementation(libs.ktor.client.okhttp)
            // QR kit
            implementation(libs.zxing)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.preview)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.datastore)
            implementation(libs.kotlinx.datetime)
            // Koin DI
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.koin.annotations)
            // KTOR
            implementation(libs.ktor.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.logging)
            // Supabase
            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.postgrest)
            // QR Kit
            implementation(libs.qrcode)
            // Decompose
            api(libs.decompose)
            implementation(libs.decompose.cmp)
            implementation(libs.decompose.cmp.experimental)
            api(libs.decompose.essenity)
            implementation(libs.decompose.essenity.coroutines)
            // Paging + compose
            implementation(libs.paging)
            implementation(libs.paging.compose)
            // Room
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.compose.ui.test)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.mockk)
            implementation(libs.mockk.agent)
            implementation(libs.koin.test)
        }

        kotzilla {
            versionName = libs.versions.packageVersion.get()
            keyGeneration = KotzillaKeyGeneration.COMPOSE
            composeInstrumentation = true
        }
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
}

android {
    namespace = "io.github.alexmaryin.followmymus"
    compileSdkVersion(libs.versions.android.compileSdk.get().toInt())

    defaultConfig {
        applicationId = "io.github.alexmaryin.followmymus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = libs.versions.android.versionCode.get().toInt()
        versionName = libs.versions.packageVersion.get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(libs.compose.ui.tooling)
    // KOIN
    add("kspCommonMainMetadata", libs.koin.ksp)
    add("kspAndroid", libs.koin.ksp)
    add("kspJvm", libs.koin.ksp)
    add("kspIosSimulatorArm64", libs.koin.ksp)
    add("kspIosArm64", libs.koin.ksp)
    // ROOM
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspJvm", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    androidTestImplementation(libs.android.test.compose)
    debugImplementation(libs.android.test.manifest)
}

// Trigger Common Metadata Generation from Native tasks
tasks.matching { it.name.startsWith("ksp") && it.name != "kspCommonMainKotlinMetadata" }.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
}

compose.desktop {
    application {
        mainClass = "io.github.alexmaryin.followmymus.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.alexmaryin.followmymus"
            packageVersion = libs.versions.packageVersion.get()
        }
    }
}

buildkonfig {
    packageName = "io.github.alexmaryin.followmymus"

    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "projectId", project.getLocalProperty("projectId"))
        buildConfigField(FieldSpec.Type.STRING, "publishableKey", project.getLocalProperty("publishableKey"))
        buildConfigField(FieldSpec.Type.STRING, "secretKey", project.getLocalProperty("secretKey"))
        buildConfigField(FieldSpec.Type.STRING, "musicBrainzOpenAuth", project.getLocalProperty("musicBrainzOpenAuth"))
        buildConfigField(FieldSpec.Type.STRING, "musicBrainzSecret", project.getLocalProperty("musicBrainzSecret"))
        buildConfigField(FieldSpec.Type.STRING, "appVersion", libs.versions.packageVersion.get())
    }
}