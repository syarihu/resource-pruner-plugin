pluginManagement {
  val versionName = providers.gradleProperty("VERSION_NAME").get()
  repositories {
    mavenLocal {
      content {
        includeGroupByRegex("net\\.syarihu\\.resource-?pruner.*")
      }
    }
    gradlePluginPortal()
    mavenCentral()
    google()
  }
  plugins {
    kotlin("jvm") version embeddedKotlinVersion
    kotlin("android") version embeddedKotlinVersion
    id("net.syarihu.resource-pruner") version versionName
  }
}

dependencyResolutionManagement {
  repositories {
    mavenLocal {
      content {
        includeGroupByRegex("net\\.syarihu\\.resource-?pruner.*")
      }
    }
    mavenCentral()
    google()
  }
  versionCatalogs {
    create("pluginLibs") { from(files("gradle/plugin.versions.toml")) }
    create("exampleLibs") { from(files("gradle/example.versions.toml")) }
  }
}

rootProject.name = "resource-pruner-plugin"

include(":resource-pruner-core")
include(":resource-pruner-gradle-plugin")

// example modules require the plugin to be published to Maven Local first
// Use -PexcludeExample to skip them during initial build
if (providers.gradleProperty("excludeExample").isPresent.not()) {
  include(":example")
  include(":example-multi-app")
  include(":example-lib")
}
