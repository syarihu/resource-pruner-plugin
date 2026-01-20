pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    google()
  }
  plugins {
    kotlin("jvm") version embeddedKotlinVersion
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

rootProject.name = "resource-pruner-plugin"

include(":resource-pruner-core")
include(":resource-pruner-gradle-plugin")
