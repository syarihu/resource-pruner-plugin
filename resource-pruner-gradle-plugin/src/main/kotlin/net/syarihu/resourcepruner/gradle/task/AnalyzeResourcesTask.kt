package net.syarihu.resourcepruner.gradle.task

import net.syarihu.resourcepruner.collector.CompositeResourceCollector
import net.syarihu.resourcepruner.detector.CompositeUsageDetector
import net.syarihu.resourcepruner.pruner.DefaultResourcePruner
import org.gradle.api.tasks.TaskAction

/**
 * Task for analyzing unused resources without removing them.
 *
 * This task scans the project's resources and source code to identify
 * resources that are not referenced anywhere, but does not remove them.
 *
 * Usage:
 * ```
 * ./gradlew analyzeResourcesDebug
 * ./gradlew analyzeResourcesRelease
 * ```
 */
abstract class AnalyzeResourcesTask : BaseResourcePrunerTask() {
  @TaskAction
  fun analyze() {
    logger.lifecycle("Analyzing resources...")

    val resDirs = resDirectories.files.map { it.toPath() }
    val sourceDirs = sourceDirectories.files.map { it.toPath() }

    logger.lifecycle("Resource directories: ${resDirs.size}")
    logger.lifecycle("Source directories: ${sourceDirs.size}")

    val excludePatterns = compileExcludePatterns()
    if (excludePatterns.isNotEmpty()) {
      logger.lifecycle("Exclude patterns: ${excludePatterns.map { it.pattern }}")
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
    val analysis = pruner.analyze(detectedResources, references, excludePatterns)

    // Report results
    logger.lifecycle("")
    logger.lifecycle("=== Analysis Results ===")
    logger.lifecycle("Total resources: ${detectedResources.size}")
    logger.lifecycle("Resources to preserve: ${analysis.toBePreserved.size}")
    logger.lifecycle("Resources to prune: ${analysis.toBeRemoved.size}")

    if (analysis.toBeRemoved.isNotEmpty()) {
      logger.lifecycle("")
      logger.lifecycle("Unused resources:")
      analysis.toBeRemoved
        .groupBy { it.type.typeName }
        .forEach { (typeName, resources) ->
          logger.lifecycle("  $typeName: ${resources.size}")
          resources.take(10).forEach { resource ->
            logger.lifecycle("    - ${resource.name}")
          }
          if (resources.size > 10) {
            logger.lifecycle("    ... and ${resources.size - 10} more")
          }
        }
    }

    logger.lifecycle("")
    logger.lifecycle("Run 'pruneResources' task to remove unused resources.")
  }
}
