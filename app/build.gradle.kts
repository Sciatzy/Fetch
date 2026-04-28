plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.fetch.auth.production"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fetch.auth.production"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BACKEND_BASE_URL", "\"https://fetch-api.onrender.com/\"")
        buildConfigField("String", "OPENROUTESERVICE_API_KEY", "\"REPLACE_WITH_OPENROUTESERVICE_API_KEY\"")
        buildConfigField("String", "PAYMONGO_SECRET_KEY", "\"REPLACE_WITH_PAYMONGO_SECRET_KEY\"")
        buildConfigField("String", "PAYMONGO_PUBLIC_KEY", "\"REPLACE_WITH_PAYMONGO_PUBLIC_KEY\"")
        buildConfigField("String", "PAYMONGO_WEBHOOK_URL", "\"REPLACE_WITH_PAYMONGO_WEBHOOK_URL\"")
        buildConfigField("String", "PAYMONGO_WEBHOOK_SECRET", "\"REPLACE_WITH_PAYMONGO_WEBHOOK_SECRET\"")
        buildConfigField("boolean", "ALLOW_RIDER_TEST_BYPASS", "true")
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
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.facebook.android:facebook-login:17.0.0")
}
