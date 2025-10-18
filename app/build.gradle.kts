plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.musicbanger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.musicbanger"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    // ✅ Viết theo Kotlin DSL (dùng ngoặc tròn)
    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.media:media:1.6.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation(libs.appcompat)

    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-hls:2.19.1")
    implementation ("com.google.android.exoplayer:exoplayer-dash:2.19.1")
    implementation("com.google.android.material:material:1.6.0")
    implementation ("androidx.media:media:1.6.0")
    implementation ("androidx.media2:media2-session:1.2.1")

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation ("com.github.bumptech.glide:glide:4.14.2")
    implementation(libs.palette)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.14.2")
}
