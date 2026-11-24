plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.petal.browser"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.petal.browser"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0-mobile"
    }
}
