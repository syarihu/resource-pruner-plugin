plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("android") apply false
  alias(exampleLibs.plugins.paraphrase)
  id("net.syarihu.resource-pruner")
}

// AGP 9 has built-in Kotlin and rejects the Kotlin Android plugin; AGP 8.x still needs it.
if (com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt() < 9) {
  apply(plugin = "org.jetbrains.kotlin.android")
}

android {
  namespace = "net.syarihu.resourcepruner.example"
  compileSdk = 37

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

// Type-safe `kotlin {}` accessor is unavailable when the plugin is applied conditionally
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
