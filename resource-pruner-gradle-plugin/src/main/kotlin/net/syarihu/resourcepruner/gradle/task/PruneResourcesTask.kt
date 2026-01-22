package net.syarihu.resourcepruner.gradle.task

import net.syarihu.resourcepruner.collector.CompositeResourceCollector
import net.syarihu.resourcepruner.detector.CompositeUsageDetector
import net.syarihu.resourcepruner.pruner.DefaultResourcePruner
import org.gradle.api.tasks.TaskAction

/**
 * Task for pruning unused resources.
 *
 * This task scans the project's resources and source code to identify
 * resources that are not referenced anywhere, and removes them.
 *
 * Usage:
 * ```
 * ./gradlew pruneResourcesDebug
 * ./gradlew pruneResourcesRelease
 * ```
 */
abstract class PruneResourcesTask : BaseResourcePrunerTask() {
  @TaskAction
  fun prune() {
    logger.lifecycle("Pruning resources...")

    val resDirs = resDirectories.files.map { it.toPath() }
    val sourceDirs = sourceDirectories.files.map { it.toPath() }

    logger.info("Resource directories: ${resDirs.size}")
    logger.info("Source directories: ${sourceDirs.size}")

    val excludePatterns = compileExcludePatterns()
    if (excludePatterns.isNotEmpty()) {
      logger.lifecycle("Exclude patterns: ${excludePatterns.map { it.pattern }}")
    }

    val targetTypes = targetResourceTypes.get()
    if (targetTypes.isNotEmpty()) {
      logger.lifecycle("Target resource types: $targetTypes")
    }

    val excludeTypes = excludeResourceTypes.get()
    if (excludeTypes.isNotEmpty()) {
      logger.lifecycle("Exclude resource types: $excludeTypes")
    }

    // Collect resources
    val collector = CompositeResourceCollector.createDefault()
    val detectedResources = collector.collect(resDirs)
    logger.lifecycle("Detected ${detectedResources.size} resources")

    // Detect usage
    val detector = CompositeUsageDetector.createDefault()
    val references = detector.detect(sourceDirs, resDirs)
    logger.lifecycle("Found ${references.size} resource references")

    // Analyze
    val pruner = DefaultResourcePruner()
    val analysis = pruner.analyze(
      detectedResources,
      references,
      excludePatterns,
      targetTypes,
      excludeTypes,
    )

    if (analysis.toBeRemoved.isEmpty()) {
      logger.lifecycle("No unused resources found. Nothing to prune.")
      return
    }

    logger.lifecycle("Found ${analysis.toBeRemoved.size} unused resources. Pruning...")

    // Execute pruning
    val report = pruner.execute(analysis)

    // Report results
    logger.lifecycle("")
    logger.lifecycle("=== Pruning Results ===")
    logger.lifecycle("Resources pruned: ${report.prunedCount}")
    logger.lifecycle("Resources preserved: ${report.preservedCount}")

    if (report.errorCount > 0) {
      logger.lifecycle("Errors: ${report.errorCount}")
      report.errors.forEach { error ->
        logger.error("  - ${error.resource.name}: ${error.message}")
      }
    }

    if (report.prunedResources.isNotEmpty()) {
      logger.lifecycle("")
      logger.lifecycle("Pruned resources:")
      report.prunedResources
        .groupBy { it.resource.type.typeName }
        .forEach { (typeName, resources) ->
          logger.lifecycle("  $typeName: ${resources.size}")
          resources.take(10).forEach { pruned ->
            logger.lifecycle("    - ${pruned.resource.name}")
          }
          if (resources.size > 10) {
            logger.lifecycle("    ... and ${resources.size - 10} more")
          }
        }
    }

    logger.lifecycle("")
    if (report.isSuccess) {
      logger.lifecycle("Pruning completed successfully!")
    } else {
      logger.warn("Pruning completed with ${report.errorCount} errors.")
    }
  }
}
