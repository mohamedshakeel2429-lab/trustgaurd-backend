plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "ai.scamchain.mobile"
    compileSdk = 36
    val scamChainApiUrl = providers.gradleProperty("scamchainApiUrl")
        .orElse(providers.environmentVariable("SCAMCHAIN_API_URL"))
        .orElse("http://10.0.2.2:8000/api/v1")

    defaultConfig {
        applicationId = "com.scamchain.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"${scamChainApiUrl.get()}\"")
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug { manifestPlaceholders["allowCleartext"] = "true" }
        release { manifestPlaceholders["allowCleartext"] = "false" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
}

dependencies {
    // Compose 1.9 stays compatible with the Android 16 (API 36) compile SDK.
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
