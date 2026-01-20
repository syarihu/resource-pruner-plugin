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

    // Register tasks for Android Application projects
    project.plugins.withType(AppPlugin::class.java) {
      val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
      registerTasks(project, androidComponents, extension)
    }

    // Register tasks for Android Library projects
    project.plugins.withType(LibraryPlugin::class.java) {
      val androidComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
      registerTasks(project, androidComponents, extension)
    }
  }

  private fun registerTasks(
    project: Project,
    androidComponents: AndroidComponentsExtension<*, *, *>,
    extension: ResourcePrunerExtension,
  ) {
    androidComponents.onVariants { variant ->
      val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }

      // Get source directories
      val sourceDirectories = getSourceDirectories(project, variant)
      val resDirectories = getResDirectories(project, variant)

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

  companion object {
    const val TASK_GROUP = "resource pruner"
  }
}
