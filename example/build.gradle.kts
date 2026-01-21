plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("android")
  alias(exampleLibs.plugins.paraphrase)
  id("net.syarihu.resource-pruner")
}

android {
  namespace = "net.syarihu.resourcepruner.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "net.syarihu.resourcepruner.example"
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

  buildFeatures {
    viewBinding = true
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  implementation(project(":example-lib"))
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.appcompat)
  implementation(exampleLibs.androidx.recyclerview)
  implementation(exampleLibs.material)
  implementation(exampleLibs.paraphrase.runtime)
}

resourcePruner {
  // Exclude launcher icons and app name
  excludeResourceNamePatterns.addAll(
    "^ic_launcher.*",
    "^app_name$",
  )
}
