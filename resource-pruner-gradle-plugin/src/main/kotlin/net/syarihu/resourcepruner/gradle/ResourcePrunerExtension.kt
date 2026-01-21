package net.syarihu.resourcepruner.gradle

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Extension for configuring the Resource Pruner plugin.
 *
 * Example usage in build.gradle.kts:
 * ```kotlin
 * resourcePruner {
 *   excludeResourceNamePatterns.addAll(
 *     "^ic_launcher.*",
 *     "^google_play_.*",
 *     "^app_name$",
 *   )
 * }
 * ```
 */
abstract class ResourcePrunerExtension {
  /**
   * Patterns (regular expressions) for resource names to exclude from pruning.
   *
   * Resources matching any of these patterns will be preserved even if unused.
   *
   * Example:
   * ```kotlin
   * excludeResourceNamePatterns.addAll(
   *   "^ic_launcher.*",  // Preserve launcher icons
   *   "^app_name$",      // Preserve app name
   * )
   * ```
   */
  abstract val excludeResourceNamePatterns: ListProperty<String>

  /**
   * Resource types to target for pruning.
   *
   * If empty, all resource types are targeted.
   *
   * Valid values: "drawable", "layout", "menu", "mipmap", "animator", "anim", "color",
   *               "string", "dimen", "style", "bool", "integer", "array", "attr", "plurals"
   *
   * Example:
   * ```kotlin
   * targetResourceTypes.addAll("drawable", "string")
   * ```
   */
  abstract val targetResourceTypes: SetProperty<String>

  /**
   * Source sets to scan for resource usage.
   *
   * Defaults to ["main"] if not specified.
   *
   * Example:
   * ```kotlin
   * sourceSets.addAll("main", "debug")
   * ```
   */
  abstract val sourceSets: SetProperty<String>

  /**
   * Whether to scan dependent projects' source code for resource usage.
   *
   * When enabled (default), library modules will scan the source code of
   * projects that depend on them to detect resource usage. This prevents
   * library resources that are used by app modules from being incorrectly
   * marked as unused.
   *
   * Defaults to true.
   *
   * Example:
   * ```kotlin
   * scanDependentProjects.set(false) // Disable cross-module scanning
   * ```
   */
  abstract val scanDependentProjects: Property<Boolean>
}
