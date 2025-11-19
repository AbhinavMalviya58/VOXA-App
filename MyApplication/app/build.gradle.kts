plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.WANGDULabs.VOXA"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.WANGDULabs.VOXA"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "VERCEL_BASE_URL", "\"https://voxa-app.vercel.app\"")
        buildConfigField("String", "VERCEL_API_PREFIX", "\"/api\"")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")
    // Also add the dependency for the Google Play services library and specify its version
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.google.firebase:firebase-crashlytics:20.0.3")
    implementation("androidx.gridlayout:gridlayout:1.1.0")
    implementation("com.google.firebase:firebase-database:22.0.1")
//    implementation("androidx.activity:activity:1.11.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.activity:activity:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation ("com.airbnb.android:lottie:6.7.0")
    implementation("com.github.AtifSayings:Animatoo:1.0.1")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.29")
    // Glide for avatar and image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    // Firestore for search and chat
    implementation("com.google.firebase:firebase-firestore")
    // Storage for profile photos
    implementation("com.google.firebase:firebase-storage")
    implementation ("com.google.firebase:firebase-functions")

}