import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "dev.stagefuel.vndb"
  compileSdk = 37

  defaultConfig {
    applicationId = "dev.stagefuel.vndb"
    minSdk = 29
    targetSdk = 36
    versionCode = 9
    versionName = "0.3.6"
  }

  signingConfigs {
    // CI signs releases with a keystore passed in through env vars; local builds fall back to debug.
    val ks = System.getenv("VNDB_KEYSTORE")
    if (ks != null) {
      create("release") {
        storeFile = file(ks)
        storePassword = System.getenv("VNDB_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("VNDB_KEY_ALIAS")
        keyPassword = System.getenv("VNDB_KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
  }
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.javaVersion.get())
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.browser)
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  implementation(libs.compose.material.icons)
  debugImplementation(libs.compose.ui.tooling)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.jsoup)
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(platform(libs.compose.bom))
  testImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.test.manifest)
}
