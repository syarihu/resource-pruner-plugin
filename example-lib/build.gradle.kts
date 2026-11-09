plugins {
  alias(pluginLibs.plugins.android.library)
  kotlin("android") apply false
  id("net.syarihu.resource-pruner")
}

// AGP 9 has built-in Kotlin and rejects the Kotlin Android plugin; AGP 8.x still needs it.
if (com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt() < 9) {
  apply(plugin = "org.jetbrains.kotlin.android")
}

android {
  namespace = "net.syarihu.resourcepruner.examplelib"
  compileSdk = 37

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

// Type-safe `kotlin {}` accessor is unavailable when the plugin is applied conditionally
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.appcompat)
}

resourcePruner {
  // Exclude common patterns
  excludeResourceNamePatterns.addAll(
    "^ic_launcher.*",
  )
}
