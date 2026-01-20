plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("android")
}

android {
  namespace = "net.syarihu.resourcepruner.examplemultiapp"
  compileSdk = 35

  defaultConfig {
    applicationId = "net.syarihu.resourcepruner.examplemultiapp"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
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

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

configurations.all {
  resolutionStrategy {
    // Force consistent Kotlin stdlib version
    force("org.jetbrains.kotlin:kotlin-stdlib:$embeddedKotlinVersion")
    force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$embeddedKotlinVersion")
  }
}

dependencies {
  implementation(project(":example-lib"))
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.appcompat)
}
