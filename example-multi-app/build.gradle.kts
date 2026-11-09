plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("android") apply false
  id("org.jetbrains.kotlin.plugin.compose")
}

// AGP 9 has built-in Kotlin and rejects the Kotlin Android plugin; AGP 8.x still needs it.
if (com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION.substringBefore('.').toInt() < 9) {
  apply(plugin = "org.jetbrains.kotlin.android")
}

android {
  namespace = "net.syarihu.resourcepruner.examplemultiapp"
  compileSdk = 37

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

  // Compose and Android resources in unit tests are enabled here as a regression test for #57
  buildFeatures {
    compose = true
  }

  testOptions {
    unitTests.isIncludeAndroidResources = true
  }
}

// Type-safe `kotlin {}` accessor is unavailable when the plugin is applied conditionally
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

// Regression guard for #57: when enabled, fail if another project's configuration realizes this
// project's tasks eagerly. configureEach only runs for tasks that get realized.
if (providers.gradleProperty("resourcePruner.assertNoEagerRealization").isPresent) {
  tasks.configureEach {
    if (name == "assembleRelease") {
      throw GradleException("Task '$path' was realized eagerly while configuring another project (#57)")
    }
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
  implementation(platform(exampleLibs.androidx.compose.bom))
  implementation(exampleLibs.androidx.compose.runtime)
  implementation(exampleLibs.androidx.compose.ui)
  implementation(exampleLibs.androidx.compose.ui.tooling.preview)
}
