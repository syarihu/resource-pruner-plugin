package net.syarihu.resourcepruner.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Base task for resource pruner operations.
 */
abstract class BaseResourcePrunerTask : DefaultTask() {
  /**
   * Patterns (regular expressions) for resource names to exclude from pruning.
   */
  @get:Input
  @get:Optional
  abstract val excludeResourceNamePatterns: ListProperty<String>

  /**
   * Resource types to target for pruning.
   * If empty, all resource types are targeted.
   */
  @get:Input
  @get:Optional
  abstract val targetResourceTypes: SetProperty<String>

  /**
   * Resource types to exclude from pruning.
   * Resources of these types will be preserved even if unused.
   */
  @get:Input
  @get:Optional
  abstract val excludeResourceTypes: SetProperty<String>

  /**
   * Resource directories to scan for resources.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val resDirectories: ConfigurableFileCollection

  /**
   * Source directories to scan for resource usage.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceDirectories: ConfigurableFileCollection

  /**
   * Compiles exclude patterns to Regex objects.
   */
  protected fun compileExcludePatterns(): List<Regex> = excludeResourceNamePatterns.get().map { it.toRegex() }
}
