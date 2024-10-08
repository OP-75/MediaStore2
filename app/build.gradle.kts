plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.mediastore2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mediastore2"
        minSdk = 31
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    //for Easy Permissions
    implementation("pub.devrel:easypermissions:3.0.0")
    implementation ("com.vmadalin:easypermissions-ktx:1.0.0")

    //Simple storage
    implementation("com.anggrayudi:storage:2.0.0")

    //Material Dialog
    implementation("com.afollestad.material-dialogs:files:3.3.0")

    //For saving objects in Shared preferences
    implementation("com.google.code.gson:gson:2.11.0")

    implementation("com.github.bumptech.glide:glide:4.14.2")
    implementation(libs.androidx.activity)
    // Skip this if you don't want to use integration libraries or configure Glide.
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("com.otaliastudios:zoomlayout:1.9.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}