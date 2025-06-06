plugins {
    // === existing ===
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.universityofreading.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.universityofreading.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Required for Retrofit and OkHttp
        isCoreLibraryDesugaringEnabled = true
        // Required for java.time APIs like YearMonth on older Android devices
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose      = true
        buildConfig  = true        // we read the Maps key via BuildConfig
    }
    
    // Fixes unresolved reference issues for retrofit2
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
            force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.10")
            // Ensure all Retrofit components use the same version
            force("com.squareup.retrofit2:retrofit:2.9.0")
            force("com.squareup.retrofit2:converter-gson:2.9.0")
            // Ensure all OkHttp components use the same version
            force("com.squareup.okhttp3:okhttp:4.11.0")
            force("com.squareup.okhttp3:logging-interceptor:4.11.0")
        }
    }
}

dependencies {
    // ---------- Android / Compose core ----------
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Material icons
    implementation("androidx.compose.material:material-icons-core:1.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Java 8+ API desugaring support
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // ---------- Map & visualisation ----------
    implementation("com.google.android.gms:play-services-maps:19.0.0")               // Google Maps
    implementation("com.google.maps.android:android-maps-utils:3.4.0")               // heat-map utils
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")                       // charts

    // ---------- Data / utils ----------
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")

    // ---------- NEW – safest‑route feature ----------
    implementation("com.google.maps:google-maps-services:2.2.0")        // Directions HTTP client
    implementation("com.github.davidmoten:rtree:0.12")                  // Spatial index for crime data
    implementation("io.reactivex:rxjava:1.3.8")                         // Required by rtree
    // -------------------------------------------------
    
    // ---------- Network - Police API ----------
    // Use implementation instead of implementation to avoid substitution
    implementation("com.squareup.retrofit2:retrofit:2.9.0")             // API client
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")       // JSON conversion
    implementation("com.squareup.okhttp3:okhttp:4.11.0")                // HTTP client
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")   // Network logging
    // -------------------------------------------------

    // ---------- Navigation ----------
    implementation("androidx.navigation:navigation-compose:2.7.2")

    // ---------- Optional examples ----------
    implementation(libs.generativeai)   // (was already here – unrelated to routing)
    
    // ---------- Testing ----------
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
