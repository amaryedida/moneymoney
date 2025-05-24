plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.money.moneymoney"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.money.moneymoney"
        minSdk = 27
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            assets.srcDirs("src/main/assets")
        }
        getByName("test") {
            java.srcDirs("src/test/java")
        }
        getByName("androidTest") {
            java.srcDirs("src/androidTest/java")
        }
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
            excludes += "META-INF/versions/9/module-info.class"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/maven/**"
            excludes += "META-INF/proguard/**"
            excludes += "META-INF/androidx.*"
            excludes += "META-INF/com.google.android.material_material.version"
            excludes += "META-INF/com.google.guava_guava.version"
            excludes += "META-INF/com.google.android.gms_play-services-auth.version"
            excludes += "META-INF/com.google.api-client_google-api-client.version"
            excludes += "META-INF/com.google.apis_google-api-services-drive.version"
            excludes += "META-INF/com.google.http-client_google-http-client.version"
            pickFirsts += "lib/armeabi-v7a/libc++_shared.so"
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
            pickFirsts += "lib/x86/libc++_shared.so"
            pickFirsts += "lib/x86_64/libc++_shared.so"
            pickFirsts += "lib/armeabi-v7a/libfbjni.so"
            pickFirsts += "lib/arm64-v8a/libfbjni.so"
            pickFirsts += "lib/x86/libfbjni.so"
            pickFirsts += "lib/x86_64/libfbjni.so"
            pickFirsts += "lib/armeabi-v7a/libfolly_runtime.so"
            pickFirsts += "lib/arm64-v8a/libfolly_runtime.so"
            pickFirsts += "lib/x86/libfolly_runtime.so"
            pickFirsts += "lib/x86_64/libfolly_runtime.so"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:32.1.3-android")
        force("org.apache.httpcomponents:httpclient:4.5.14")
        force("com.google.android.gms:play-services-auth:21.2.0")
        force("com.google.api-client:google-api-client-android:2.8.0")
        force("com.google.apis:google-api-services-drive:v3-rev20250511-2.0.0")
        force("com.google.http-client:google-http-client-android:1.43.3")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.google.android.material:material:1.12.0")
    
    // Google Sign-In and Drive API dependencies with exclusions
    implementation("com.google.android.gms:play-services-auth:21.2.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("com.google.api-client:google-api-client-android:2.8.0") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("com.google.http-client:google-http-client-android:1.43.3") {
        exclude(group = "com.google.guava", module = "guava")
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    
    // Add explicit guava dependency to resolve conflicts
    implementation("com.google.guava:guava:32.1.3-android")
}