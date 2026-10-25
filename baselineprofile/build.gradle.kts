plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "app.stova.baselineprofile"
    compileSdk = 36
    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":app"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile { useConnectedDevices = true }

dependencies {
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test:runner:1.7.0")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
}
