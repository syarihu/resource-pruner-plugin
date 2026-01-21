plugins {
  kotlin("jvm")
  alias(libs.plugins.dokka)
  alias(libs.plugins.nmcp)
  `java-gradle-plugin`
  `maven-publish`
  signing
}

group = "net.syarihu.resourcepruner"
version = findProperty("VERSION_NAME") as String

kotlin {
  jvmToolchain(17)
}

java {
  withSourcesJar()
}

dependencies {
  implementation(project(":resource-pruner-core"))
  compileOnly(pluginLibs.android.gradle.plugin)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

gradlePlugin {
  plugins {
    create("resourcePruner") {
      id = "net.syarihu.resource-pruner"
      implementationClass = "net.syarihu.resourcepruner.gradle.ResourcePrunerPlugin"
      displayName = "Resource Pruner Plugin"
      description =
        "A Gradle plugin that acts as your project's gardener, carefully pruning unused " +
        "resources from your Android codebase to keep it clean and maintainable"
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

publishing {
  publications {
    // java-gradle-plugin creates "pluginMaven" publication automatically
    withType<MavenPublication>().configureEach {
      if (name == "pluginMaven") {
        artifact(dokkaJavadocJar)
      }

      pom {
        name.set("Resource Pruner Gradle Plugin")
        description.set(
          "A Gradle plugin that acts as your project's gardener, carefully pruning unused " +
            "resources from your Android codebase to keep it clean and maintainable",
        )
        url.set("https://github.com/syarihu/resource-pruner-plugin")

        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("syarihu")
            name.set("syarihu")
            url.set("https://github.com/syarihu")
          }
        }

        scm {
          url.set("https://github.com/syarihu/resource-pruner-plugin")
          connection.set("scm:git:git://github.com/syarihu/resource-pruner-plugin.git")
          developerConnection.set("scm:git:ssh://git@github.com/syarihu/resource-pruner-plugin.git")
        }
      }
    }
  }
}

signing {
  val signingKey = findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
  val signingPassword = findProperty("signingPassword")?.toString() ?: System.getenv("SIGNING_PASSWORD")

  if (signingKey != null && signingPassword != null) {
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
  }
}
