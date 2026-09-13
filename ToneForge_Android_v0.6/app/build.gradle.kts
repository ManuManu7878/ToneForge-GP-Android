plugins { id("com.android.application") }

android {
    namespace = "com.toneforge.gp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.toneforge.gp"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "0.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
