package net.syarihu.resourcepruner.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import net.syarihu.resourcepruner.gradle.task.AnalyzeResourcesTask
import net.syarihu.resourcepruner.gradle.task.PruneResourcesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import java.io.File

/**
 * Gradle plugin for pruning unused Android resources.
 *
 * This plugin acts as a gardener for your codebase, carefully pruning unused resources
 * to keep your project clean and maintainable.
 *
 * Usage:
 * ```kotlin
 * plugins {
 *   id("net.syarihu.resource-pruner")
 * }
 *
 * resourcePruner {
 *   excludeNames.addAll("^ic_launcher.*", "^app_name$")
 * }
 * ```
 *
 * Tasks:
 * - `analyzeResources{Variant}`: Analyze unused resources without removing them
 * - `pruneResources{Variant}`: Remove unused resources
 */
class ResourcePrunerPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = project.extensions.create(
      "resourcePruner",
      ResourcePrunerExtension::class.java,
    )

    // Set default values
    extension.sourceSets.convention(setOf("main"))
    extension.excludeNames.convention(emptyList())
    extension.targetResourceTypes.convention(emptySet())
    extension.scanDependentProjects.convention(true)


    // Register tasks for Android Application projects
    project.plugins.withType(AppPlugin::class.java) {
      val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
      registerTasks(project, androidComponents, extension, isLibrary = false)
    }

    // Register tasks for Android Library projects
    project.plugins.withType(LibraryPlugin::class.java) {
      val androidComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
      registerTasks(project, androidComponents, extension, isLibrary = true)
    }
  }

  private fun registerTasks(
    project: Project,
    androidComponents: AndroidComponentsExtension<*, *, *>,
    extension: ResourcePrunerExtension,
    isLibrary: Boolean,
  ) {
    androidComponents.onVariants { variant ->
      val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

      // Get source directories
      val sourceDirectories = getSourceDirectories(project, variant)
      val resDirectories = getResDirectories(project, variant)

      // For library modules, also scan dependent projects' sources
      val dependentProjectSources = if (isLibrary) {
        project.provider {
          if (extension.scanDependentProjects.get()) {
            getDependentProjectSourceDirectories(project, variant.name)
          } else {
            emptyList()
          }
        }
      } else {
        project.provider { emptyList<File>() }
      }

      // Register analyze task
      project.tasks.register(
        "analyzeResources$variantName",
        AnalyzeResourcesTask::class.java,
      ) { task ->
        task.group = TASK_GROUP
        task.description = "Analyze unused resources for $variantName variant"
        task.excludeNames.set(extension.excludeNames)
        task.targetResourceTypes.set(extension.targetResourceTypes)
        task.sourceDirectories.from(sourceDirectories)
        task.sourceDirectories.from(dependentProjectSources)
        task.resDirectories.from(resDirectories)
      }

      // Register prune task
      project.tasks.register(
        "pruneResources$variantName",
        PruneResourcesTask::class.java,
      ) { task ->
        task.group = TASK_GROUP
        task.description = "Prune unused resources for $variantName variant"
        task.excludeNames.set(extension.excludeNames)
        task.targetResourceTypes.set(extension.targetResourceTypes)
        task.sourceDirectories.from(sourceDirectories)
        task.sourceDirectories.from(dependentProjectSources)
        task.resDirectories.from(resDirectories)
      }
    }
  }

  /**
   * Gets the source directories for a variant.
   */
  private fun getSourceDirectories(
    project: Project,
    variant: Variant,
  ): List<Any> {
    val sources = mutableListOf<Any>()

    // Add Kotlin/Java source directories
    variant.sources.kotlin?.let { kotlinSources ->
      sources.add(kotlinSources.all)
    }
    variant.sources.java?.let { javaSources ->
      sources.add(javaSources.all)
    }

    // Add manifest directory (contains AndroidManifest.xml which references resources)
    variant.sources.manifests.let { manifestSources ->
      sources.add(manifestSources.all)
    }

    return sources
  }

  /**
   * Gets the resource directories for a variant.
   */
  private fun getResDirectories(
    project: Project,
    variant: Variant,
  ): List<Any> {
    val resources = mutableListOf<Any>()

    // Add res directories
    variant.sources.res?.let { resSources ->
      resources.add(resSources.all)
    }

    return resources
  }

  /**
   * Finds all projects that depend on this project and returns their source directories.
   *
   * This enables library modules to detect resource usage in dependent app/library modules.
   */
  private fun getDependentProjectSourceDirectories(
    project: Project,
    variantName: String,
  ): List<File> {
    val dependentSources = mutableListOf<File>()
    val rootProject = project.rootProject

    // Find all projects that have a dependency on this project
    rootProject.allprojects.forEach { otherProject ->
      if (otherProject == project) return@forEach

      // Check if otherProject depends on this project
      // Try multiple approaches to detect dependencies

      var dependsOnThisProject = false

      // Approach 1: Check Gradle configurations (works when project is fully configured)
      val commonConfigs = listOf(
        "implementation",
        "api",
        "compileOnly",
        "runtimeOnly",
        "debugImplementation",
        "releaseImplementation",
      )

      for (configName in commonConfigs) {
        val config = otherProject.configurations.findByName(configName)
        if (config != null) {
          val projectDeps = config.dependencies.filterIsInstance<ProjectDependency>()
          if (projectDeps.any { it.path == project.path }) {
            dependsOnThisProject = true
            break
          }
        }
      }

      // Approach 2: Parse build files directly (works with Configuration on demand)
      if (!dependsOnThisProject) {
        dependsOnThisProject = checkBuildFileForDependency(otherProject, project)
      }

      if (dependsOnThisProject) {
        // Add source directories from the dependent project
        val srcMainKotlin = otherProject.file("src/main/kotlin")
        val srcMainJava = otherProject.file("src/main/java")
        val srcVariantKotlin = otherProject.file("src/$variantName/kotlin")
        val srcVariantJava = otherProject.file("src/$variantName/java")
        val manifestFile = otherProject.file("src/main/AndroidManifest.xml")

        if (srcMainKotlin.exists()) dependentSources.add(srcMainKotlin)
        if (srcMainJava.exists()) dependentSources.add(srcMainJava)
        if (srcVariantKotlin.exists()) dependentSources.add(srcVariantKotlin)
        if (srcVariantJava.exists()) dependentSources.add(srcVariantJava)
        if (manifestFile.exists()) dependentSources.add(manifestFile)

        // Also add generated sources (ViewBinding, Paraphrase, etc.)
        val generatedDataBinding = otherProject.file(
          "build/generated/data_binding_base_class_source_out/$variantName/out",
        )
        val generatedParaphrase = otherProject.file(
          "build/generated/source/paraphrase/$variantName",
        )

        if (generatedDataBinding.exists()) dependentSources.add(generatedDataBinding)
        if (generatedParaphrase.exists()) dependentSources.add(generatedParaphrase)
      }
    }

    return dependentSources
  }

  /**
   * Checks if a project's build file contains a dependency on another project.
   *
   * This is a fallback approach that works even when Configuration on demand
   * prevents the dependent project from being fully configured.
   */
  private fun checkBuildFileForDependency(
    dependentProject: Project,
    targetProject: Project,
  ): Boolean {
    // Check both Kotlin DSL and Groovy DSL build files
    val buildFiles = listOf(
      dependentProject.file("build.gradle.kts"),
      dependentProject.file("build.gradle"),
    )

    // The project path in build files is typically like ":example-lib" or "example-lib"
    val projectName = targetProject.path // e.g., ":example-lib"
    val projectNameWithoutColon = projectName.removePrefix(":") // e.g., "example-lib"

    // Patterns to match project dependencies in build files
    val patterns = listOf(
      // Kotlin DSL: project(":example-lib") or project(":example-lib")
      """project\s*\(\s*["']:?${Regex.escape(projectNameWithoutColon)}["']\s*\)""",
      // Groovy DSL: project(':example-lib') or project(":example-lib")
      """project\s*\(\s*['"]?:?${Regex.escape(projectNameWithoutColon)}['"]?\s*\)""",
    )

    for (buildFile in buildFiles) {
      if (buildFile.exists() && buildFile.isFile) {
        try {
          val content = buildFile.readText()
          for (pattern in patterns) {
            if (Regex(pattern).containsMatchIn(content)) {
              return true
            }
          }
        } catch (e: Exception) {
          // Ignore read errors
        }
      }
    }

    return false
  }

  companion object {
    const val TASK_GROUP = "resource pruner"
  }
}
