package net.syarihu.resourcepruner.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import net.syarihu.resourcepruner.gradle.task.PruneResourcesPreviewTask
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
 *   excludeResourceNamePatterns.addAll("^ic_launcher.*", "^app_name$")
 * }
 * ```
 *
 * Tasks:
 * - `pruneResourcesPreview{Variant}`: Preview unused resources without removing them
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
    extension.excludeResourceNamePatterns.convention(emptyList())
    extension.targetResourceTypes.convention(emptySet())
    extension.excludeResourceTypes.convention(emptySet())
    extension.scanDependentProjects.convention(true)

    // Register tasks for Android Application projects
    project.plugins.withId("com.android.application") {
      val androidComponents = project.extensions
        .getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
      registerTasks(project, androidComponents, extension, isLibrary = false)
    }

    // Register tasks for Android Library projects
    project.plugins.withId("com.android.library") {
      val androidComponents = project.extensions
        .getByName("androidComponents") as AndroidComponentsExtension<*, *, *>
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
      val sourceDirectories = getSourceDirectories(variant)
      val resDirectories = getResDirectories(variant)

      // For library modules, also scan dependent projects' sources
      val dependentProjectSources = if (isLibrary) {
        project.provider {
          if (extension.scanDependentProjects.get()) {
            getDependentProjectSourceDirectories(project, variantName.lowercase())
          } else {
            emptyList()
          }
        }
      } else {
        project.provider { emptyList() }
      }

      // Register preview task
      project.tasks.register(
        "pruneResourcesPreview$variantName",
        PruneResourcesPreviewTask::class.java,
      ) { task ->
        task.group = TASK_GROUP
        task.description = "Preview unused resources for $variantName variant"
        task.excludeResourceNamePatterns.set(extension.excludeResourceNamePatterns)
        task.targetResourceTypes.set(extension.targetResourceTypes)
        task.excludeResourceTypes.set(extension.excludeResourceTypes)
        task.sourceDirectories.from(sourceDirectories)
        task.sourceDirectories.from(dependentProjectSources)
        task.resDirectories.from(resDirectories)

        // Add dependency on code generation tasks if they exist
        configureGeneratedCodeDependencies(project, task, variantName, isLibrary, extension)
      }

      // Register prune task
      project.tasks.register(
        "pruneResources$variantName",
        PruneResourcesTask::class.java,
      ) { task ->
        task.group = TASK_GROUP
        task.description = "Prune unused resources for $variantName variant"
        task.excludeResourceNamePatterns.set(extension.excludeResourceNamePatterns)
        task.targetResourceTypes.set(extension.targetResourceTypes)
        task.excludeResourceTypes.set(extension.excludeResourceTypes)
        task.sourceDirectories.from(sourceDirectories)
        task.sourceDirectories.from(dependentProjectSources)
        task.resDirectories.from(resDirectories)

        // Add dependency on code generation tasks if they exist
        configureGeneratedCodeDependencies(project, task, variantName, isLibrary, extension)
      }
    }
  }

  /**
   * Configures dependencies on code generation tasks.
   *
   * This ensures that generated code directories (Paraphrase, DataBinding, etc.)
   * are populated before the resource pruner scans them.
   */
  private fun configureGeneratedCodeDependencies(
    project: Project,
    task: org.gradle.api.Task,
    variantName: String,
    isLibrary: Boolean,
    extension: ResourcePrunerExtension,
  ) {
    // Current project's code generation tasks
    project.tasks.findByName("generateFormattedResources$variantName")?.let { paraphraseTask ->
      task.dependsOn(paraphraseTask)
    }
    project.tasks.findByName("dataBindingGenBaseClasses$variantName")?.let { dataBindingTask ->
      task.dependsOn(dataBindingTask)
    }

    // For library modules, add dependencies on dependent projects' code generation tasks
    if (isLibrary && extension.scanDependentProjects.get()) {
      val rootProject = project.rootProject

      // Use collectAllDependentProjects to match getDependentProjectSourceDirectories behavior
      val allDependents = collectAllDependentProjects(project, rootProject)

      for (dependentProject in allDependents) {
        val paraphraseTaskName = "generateFormattedResources$variantName"
        val dataBindingTaskName = "dataBindingGenBaseClasses$variantName"

        // Try to add dependency if task already exists
        dependentProject.tasks.findByName(paraphraseTaskName)?.let { task.dependsOn(it) }
        dependentProject.tasks.findByName(dataBindingTaskName)?.let { task.dependsOn(it) }

        // Also listen for task additions to catch tasks registered later
        dependentProject.tasks.whenTaskAdded { addedTask ->
          if (addedTask.name == paraphraseTaskName || addedTask.name == dataBindingTaskName) {
            task.dependsOn(addedTask)
          }
        }
      }
    }
  }

  /**
   * Checks if a project depends on another project.
   */
  private fun projectDependsOn(
    dependentProject: Project,
    targetProject: Project,
  ): Boolean {
    // Check Gradle configurations (works when project is fully configured)
    val commonConfigs = listOf(
      "implementation",
      "api",
      "compileOnly",
      "runtimeOnly",
      "debugImplementation",
      "releaseImplementation",
    )

    for (configName in commonConfigs) {
      val config = dependentProject.configurations.findByName(configName)
      if (config != null) {
        val projectDeps = config.dependencies.filterIsInstance<ProjectDependency>()
        if (projectDeps.any { it.path == targetProject.path }) {
          return true
        }
      }
    }

    // Parse build files directly (works with Configuration on demand)
    return checkBuildFileForDependency(dependentProject, targetProject)
  }

  /**
   * Finds all projects that depend on this project (including transitive dependents)
   * and returns their source directories.
   *
   * This enables library modules to detect resource usage in dependent app/library modules.
   * It recursively follows the dependency chain to find all modules that may reference
   * resources from this project.
   */
  private fun getDependentProjectSourceDirectories(
    project: Project,
    variantName: String,
  ): List<File> {
    val dependentSources = mutableListOf<File>()
    val rootProject = project.rootProject

    // Collect all dependent projects including transitive ones
    val allDependents = collectAllDependentProjects(project, rootProject)

    // Add source directories from all dependent projects
    for (dependentProject in allDependents) {
      val srcMainKotlin = dependentProject.file("src/main/kotlin")
      val srcMainJava = dependentProject.file("src/main/java")
      val srcVariantKotlin = dependentProject.file("src/$variantName/kotlin")
      val srcVariantJava = dependentProject.file("src/$variantName/java")
      val manifestFile = dependentProject.file("src/main/AndroidManifest.xml")

      if (srcMainKotlin.exists()) dependentSources.add(srcMainKotlin)
      if (srcMainJava.exists()) dependentSources.add(srcMainJava)
      if (srcVariantKotlin.exists()) dependentSources.add(srcVariantKotlin)
      if (srcVariantJava.exists()) dependentSources.add(srcVariantJava)
      if (manifestFile.exists()) dependentSources.add(manifestFile)

      // Also add generated sources (ViewBinding, Paraphrase, etc.)
      val generatedDataBinding = dependentProject.file(
        "build/generated/data_binding_base_class_source_out/$variantName/out",
      )
      val generatedParaphrase = dependentProject.file(
        "build/generated/source/paraphrase/$variantName",
      )

      if (generatedDataBinding.exists()) dependentSources.add(generatedDataBinding)
      if (generatedParaphrase.exists()) dependentSources.add(generatedParaphrase)

      // Add res directories from dependent projects
      // This is important for detecting resource references in XML files (styles, layouts, etc.)
      val resMain = dependentProject.file("src/main/res")
      val resVariant = dependentProject.file("src/$variantName/res")

      if (resMain.exists()) dependentSources.add(resMain)
      if (resVariant.exists()) dependentSources.add(resVariant)
    }

    return dependentSources
  }

  /**
   * Collects all projects that depend on the target project, including transitive dependents.
   *
   * For example, if A depends on B, and B depends on C (the target), this function
   * returns both A and B when called with C as the target.
   */
  private fun collectAllDependentProjects(
    targetProject: Project,
    rootProject: Project,
  ): Set<Project> {
    val allDependents = mutableSetOf<Project>()
    val visited = mutableSetOf<Project>()

    // Find direct dependents of the target project
    val directDependents = mutableSetOf<Project>()
    rootProject.allprojects.forEach { otherProject ->
      if (otherProject != targetProject && projectDependsOn(otherProject, targetProject)) {
        directDependents.add(otherProject)
      }
    }

    allDependents.addAll(directDependents)
    visited.addAll(directDependents)
    visited.add(targetProject)

    // Recursively find projects that depend on the direct dependents
    var currentLevel = directDependents
    while (currentLevel.isNotEmpty()) {
      val nextLevel = mutableSetOf<Project>()
      for (dependentProject in currentLevel) {
        rootProject.allprojects.forEach { otherProject ->
          if (!visited.contains(otherProject) && projectDependsOn(otherProject, dependentProject)) {
            allDependents.add(otherProject)
            nextLevel.add(otherProject)
            visited.add(otherProject)
          }
        }
      }
      currentLevel = nextLevel
    }

    return allDependents
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
    val projectName = targetProject.path // e.g., ":cross_domain:resources"
    val projectNameWithoutColon = projectName.removePrefix(":") // e.g., "cross_domain:resources"

    // Generate variable name patterns from project path
    // e.g., ":cross_domain:resources" -> ["CrossDomain.resources", "crossDomain.resources"]
    val variablePatterns = generateVariablePatterns(projectNameWithoutColon)

    // Patterns to match project dependencies in build files
    val directPatterns = mutableListOf(
      // Kotlin DSL: project(":example-lib")
      """project\s*\(\s*["']:?${Regex.escape(projectNameWithoutColon)}["']\s*\)""",
      // Groovy DSL: project(':example-lib')
      """project\s*\(\s*['"]?:?${Regex.escape(projectNameWithoutColon)}['"]?\s*\)""",
    )

    // Add variable reference patterns
    // e.g., project(Modules.CrossDomain.resources) or project(CrossDomain.resources)
    for (varPattern in variablePatterns) {
      directPatterns.add("""project\s*\(\s*(?:\w+\.)*${Regex.escape(varPattern)}\s*\)""")
    }

    for (buildFile in buildFiles) {
      if (buildFile.exists() && buildFile.isFile) {
        try {
          val content = buildFile.readText()
          for (pattern in directPatterns) {
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

  /**
   * Generates variable name patterns from a project path.
   *
   * e.g., "cross_domain:resources" -> ["CrossDomain.resources", "crossDomain.resources"]
   * e.g., "feature:home" -> ["Feature.home", "feature.home"]
   */
  private fun generateVariablePatterns(projectPath: String): List<String> {
    val patterns = mutableListOf<String>()

    // Split by colon to get path segments
    val segments = projectPath.split(":")

    if (segments.size >= 2) {
      // Convert segments to variable name format
      // e.g., ["cross_domain", "resources"] -> "CrossDomain.resources"
      val objectName = segments.dropLast(1).joinToString(".") { segment ->
        // Convert snake_case to PascalCase
        segment.split("_").joinToString("") { part ->
          part.replaceFirstChar { it.uppercaseChar() }
        }
      }
      val propertyName = segments.last()

      // Add PascalCase pattern (e.g., CrossDomain.resources)
      patterns.add("$objectName.$propertyName")

      // Add camelCase pattern (e.g., crossDomain.resources)
      val camelCaseObjectName = objectName.replaceFirstChar { it.lowercaseChar() }
      patterns.add("$camelCaseObjectName.$propertyName")
    }

    return patterns
  }

  /**
   * Gets the source directories for a variant.
   */
  private fun getSourceDirectories(variant: Variant): List<Any> {
    val sources = mutableListOf<Any>()
    variant.sources.kotlin?.all?.let { sources.add(it) }
    variant.sources.java?.all?.let { sources.add(it) }
    sources.add(variant.sources.manifests.all)
    return sources
  }

  /**
   * Gets the resource directories for a variant.
   */
  private fun getResDirectories(variant: Variant): List<Any> {
    val resources = mutableListOf<Any>()
    variant.sources.res?.all?.let { resources.add(it) }
    return resources
  }

  companion object {
    const val TASK_GROUP = "resource pruner"
  }
}
