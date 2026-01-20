plugins {
  alias(pluginLibs.plugins.android.library)
  kotlin("android")
  id("net.syarihu.resource-pruner")
}

android {
  namespace = "net.syarihu.resourcepruner.examplelib"
  compileSdk = 35

  defaultConfig {
    minSdk = 24
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
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.appcompat)
}

resourcePruner {
  // Exclude common patterns
  excludeNames.addAll(
    "^ic_launcher.*",
  )
}
