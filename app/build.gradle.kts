plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.menurepro"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.example.menurepro"
    minSdk = 24
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation("androidx.activity:activity-compose:1.11.0")
  implementation("androidx.compose.material3:material3:${property("material3Version")}")
}
