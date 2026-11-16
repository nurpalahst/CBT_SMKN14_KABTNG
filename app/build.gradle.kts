plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "id.sch.smkn14kabupatentangerang.cbt"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.sch.smkn14tangerang.cbtv3"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "3.1"
    }

    signingConfigs {
        create("smkn14ReleaseKey") {
            storeFile = file("smkn14_release.jks")
            storePassword = "smkn14jaya"
            keyAlias = "smkn14key"
            keyPassword = "smkn14jaya"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("smkn14ReleaseKey")
        }
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("smkn14ReleaseKey")
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
}
