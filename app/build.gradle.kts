plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.phamnhantucode.photoeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phamnhantucode.photoeditor"
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(files("C:\\Users\\ASUS\\Downloads\\htextview-fall-release.aar"))
    implementation(files("C:\\Users\\ASUS\\Downloads\\htextview-base-release.aar"))
    implementation(files("C:\\Users\\ASUS\\Downloads\\htextview-scale-release.aar"))
    implementation(files("C:\\Users\\ASUS\\Downloads\\htextview-typer-release.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //libs
    implementation(libs.fotoapparat)
    implementation(libs.ucrop)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.effects)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.androidphotofilters)
    implementation(libs.compressor)
    implementation(libs.glide)
    implementation(libs.ucrop)
    implementation(libs.colorpicker)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.storage)
    implementation(libs.gson)
    implementation(libs.gpuimage)
    implementation(libs.lottie)
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.firebase.ui.storage)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    implementation("com.github.Dimezis:BlurView:version-2.0.6")
}