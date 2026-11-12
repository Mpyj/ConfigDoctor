plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.configdoctor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.configdoctor"
        minSdk = 24 // برای پشتیبانی از اندروید ۷ به بالا (که با اندروید ۱۵ هم سازگاره)
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // ۱. تنظیمات امضای دیباگ (همون که قبلاً داشتیم)
    signingConfigs {
        getByName("debug") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        // ۲. تنظیمات امضای Release (این رو باید خودت بسازی - پایین‌تر توضیح دادم)
        create("release") {
            storeFile = file("${rootDir}/release.keystore")
            storePassword = "YOUR_RELEASE_PASSWORD" // این رو خودت عوض کن
            keyAlias = "configdoctor"
            keyPassword = "YOUR_RELEASE_PASSWORD" // این رو هم خودت عوض کن
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        
        // بیلد Release برای گیت‌هاب (بدون Minify که خرابکاری نکنه)
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false // فعلاً خاموش تا مطمئن شیم Xray کار می‌کنه
            isShrinkResources = false
        }

        // بیلد Release برای گوگل پلی (با Minify و Proguard)
        create("releaseStable") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    // ... (همه‌ی وابستگی‌های قبلی رو اینجا دوباره نذارم، از فایل قبلی کپی کن) ...
    implementation(files("libs/libv2ray.aar"))
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}