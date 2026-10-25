import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("androidx.baselineprofile")
}

val releaseSigning = Properties().apply {
    val file = rootProject.file("signing.properties")
    if (file.isFile) file.reader(Charsets.UTF_8).use(::load)
}
val releaseKeyFile = releaseSigning.getProperty("storeFile")?.takeIf(String::isNotBlank)?.let { rootProject.file(it) }
val releaseSigningConfigured = listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all {
    !releaseSigning.getProperty(it).isNullOrBlank()
} && releaseKeyFile?.isFile == true

abstract class VerifyReleaseSigning : DefaultTask() {
    @get:Input abstract val configured: Property<Boolean>

    @TaskAction fun verify() {
        check(configured.get()) {
            "Release krev signing.properties og Stova-nøkkelen. Ikkje lag ein ny nøkkel. Sjå docs/RELEASE_SIGNING.md."
        }
    }
}

val verifyReleaseSigning = tasks.register<VerifyReleaseSigning>("verifyReleaseSigning") {
    group = "verification"
    description = "Refuses a production build without the owner's existing signing key."
    configured.set(releaseSigningConfigured)
}
tasks.matching { it.name in setOf("assembleRelease", "bundleRelease", "packageRelease", "validateSigningRelease", "signReleaseBundle") }
    .configureEach { dependsOn(verifyReleaseSigning) }

android {
    namespace = "app.stova"

    compileSdk = 36

    defaultConfig {
        applicationId = "app.stova"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.create("stableRelease") {
                    storeFile = releaseKeyFile
                    storePassword = releaseSigning.getProperty("storePassword")
                    keyAlias = releaseSigning.getProperty("keyAlias")
                    keyPassword = releaseSigning.getProperty("keyPassword")
                }
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    testOptions {
        unitTests {
            // Robolectric needs the merged resources and the manifest to stand up a real window.
            isIncludeAndroidResources = true
            // -Pstova.screens=true renders the screenshot tests to app/build/screens; off by default.
            val screens = providers.gradleProperty("stova.screens").orElse("false")
            all {
                it.systemProperty("robolectric.logging.enabled", "false")
                it.systemProperty("stova.screens", screens.get())
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }

    // Every sentence in the UI comes from a string resource, in nynorsk and English.
    lint {
        error += "HardcodedText"
    }
}

// The profile plugin creates its build types in finalizeDsl, after the android block.
androidComponents.finalizeDsl { extension ->
    listOf("debug", "nonMinifiedRelease", "benchmarkRelease").forEach { variant ->
        extension.sourceSets.getByName(variant).apply {
            manifest.srcFile(if (variant == "debug") "src/debug/AndroidManifest.xml" else "src/performance/AndroidManifest.xml")
        }
    }
}
androidComponents.onVariants { variant ->
    if (variant.buildType in setOf("debug", "nonMinifiedRelease", "benchmarkRelease")) {
        variant.sources.kotlin!!.addStaticSourceDirectory("src/performance/java")
    }
}

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    baselineProfile(project(":baselineprofile"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt.coil3:coil-compose:3.4.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")

    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
