package net.syarihu.resourcepruner.gradle.task

import net.syarihu.resourcepruner.collector.CompositeResourceCollector
import net.syarihu.resourcepruner.detector.CompositeUsageDetector
import net.syarihu.resourcepruner.pruner.DefaultResourcePruner
import net.syarihu.resourcepruner.model.PruneError
import net.syarihu.resourcepruner.model.PrunedResource
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

    val isCascade = cascadePrune.getOrElse(false)
    if (isCascade) {
      logger.lifecycle("Cascade pruning enabled (max $maxCascadeIterations iterations)")
    }

    val collector = CompositeResourceCollector.createDefault()
    val detector = CompositeUsageDetector.createDefault()
    val pruner = DefaultResourcePruner()

    var totalPrunedCount = 0
    var totalErrorCount = 0
    var iteration = 0
    val allPrunedResources = mutableListOf<PrunedResource>()
    val allErrors = mutableListOf<PruneError>()

    do {
      iteration++
      if (isCascade && iteration > 1) {
        logger.lifecycle("")
        logger.lifecycle("=== Cascade iteration $iteration ===")
      }

      // Collect resources
      val detectedResources = collector.collect(resDirs)
      logger.lifecycle("Detected ${detectedResources.size} resources")

      // Detect usage
      val references = detector.detect(sourceDirs, resDirs)
      logger.lifecycle("Found ${references.size} resource references")

      // Analyze
      val analysis = pruner.analyze(
        detectedResources,
        references,
        excludePatterns,
        targetTypes,
        excludeTypes,
      )

      if (analysis.toBeRemoved.isEmpty()) {
        if (iteration == 1) {
          logger.lifecycle("No unused resources found. Nothing to prune.")
        } else {
          logger.lifecycle("No more unused resources found.")
        }
        break
      }

      logger.lifecycle("Found ${analysis.toBeRemoved.size} unused resources. Pruning...")

      // Execute pruning
      val report = pruner.execute(analysis)

      totalPrunedCount += report.prunedCount
      totalErrorCount += report.errorCount
      allPrunedResources.addAll(report.prunedResources)
      allErrors.addAll(report.errors)

      if (isCascade) {
        logger.lifecycle("Iteration $iteration: pruned ${report.prunedCount} resources")
      }
    } while (isCascade && iteration < maxCascadeIterations && totalPrunedCount > 0)

    // Report results
    logger.lifecycle("")
    logger.lifecycle("=== Pruning Results ===")
    if (isCascade && iteration > 1) {
      logger.lifecycle("Total iterations: $iteration")
    }
    logger.lifecycle("Resources pruned: $totalPrunedCount")

    if (totalErrorCount > 0) {
      logger.lifecycle("Errors: $totalErrorCount")
      allErrors.forEach { error ->
        logger.error("  - ${error.resource.name}: ${error.message}")
      }
    }

    if (allPrunedResources.isNotEmpty()) {
      logger.lifecycle("")
      logger.lifecycle("Pruned resources:")
      allPrunedResources
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
    if (totalErrorCount == 0) {
      logger.lifecycle("Pruning completed successfully!")
    } else {
      logger.warn("Pruning completed with $totalErrorCount errors.")
    }
  }
}
