package net.syarihu.resourcepruner.gradle.task

import net.syarihu.resourcepruner.collector.CompositeResourceCollector
import net.syarihu.resourcepruner.detector.CompositeUsageDetector
import net.syarihu.resourcepruner.gradle.model.DetectionResult
import net.syarihu.resourcepruner.gradle.model.UnusedResourceEntry
import net.syarihu.resourcepruner.model.PruneError
import net.syarihu.resourcepruner.model.PrunedResource
import net.syarihu.resourcepruner.pruner.DefaultResourcePruner
import net.syarihu.resourcepruner.pruner.PruneAnalysis
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Aggregate task that collects detection results from all variants
 * and prunes resources that are unused across ALL variants.
 *
 * This task is safe to use with parallel Gradle builds because it
 * performs all deletions in a single task, avoiding race conditions
 * between variant-specific prune tasks and merge tasks.
 *
 * Usage:
 * ```
 * ./gradlew pruneResources        # Prune unused resources across all variants
 * ./gradlew pruneResourcesPreview  # Preview without deleting
 * ```
 */
@DisableCachingByDefault(because = "Aggregate resource pruning modifies external files and should not be cached")
abstract class AggregatePruneResourcesTask : BaseResourcePrunerTask() {
  /**
   * Detection result files from all variant detection tasks.
   */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val detectionResultFiles: ConfigurableFileCollection

  /**
   * When true, only previews the resources to be pruned without deleting them.
   */
  @get:Input
  @get:Optional
  abstract val previewOnly: Property<Boolean>

  @TaskAction
  fun aggregatePrune() {
    val isPreview = previewOnly.getOrElse(false)

    if (isPreview) {
      logger.lifecycle("Previewing aggregate resource pruning...")
    } else {
      logger.lifecycle("Aggregating resource pruning across all variants...")
    }

    // Read all detection results
    val resultFiles = detectionResultFiles.files
    val missingFiles = resultFiles.filter { !it.exists() }
    if (missingFiles.isNotEmpty()) {
      throw org.gradle.api.GradleException(
        "Missing detection result files: ${missingFiles.joinToString { it.path }}. " +
          "Ensure all detection tasks have run successfully.",
      )
    }
    if (resultFiles.isEmpty()) {
      logger.lifecycle("No detection results found. Run detection tasks first.")
      return
    }

    val detectionResults = resultFiles.sortedBy { it.path }.map { DetectionResult.readFrom(it) }
    logger.lifecycle("Loaded detection results from ${detectionResults.size} variants: ${detectionResults.map { it.variantName }}")

    // Compute intersection: resources unused in ALL variants
    // Start from the smallest set and use retainAll for efficiency
    val unusedSets = detectionResults.map { result ->
      result.unusedResources.map { it.serialize() }.toSet()
    }

    val intersection = if (unusedSets.isEmpty()) {
      emptySet()
    } else {
      val smallestSet = unusedSets.minBy { it.size }
      val result = smallestSet.toMutableSet()
      for (set in unusedSets) {
        if (set !== smallestSet) {
          result.retainAll(set)
        }
      }
      result.toSet()
    }

    logger.lifecycle("Resources unused across all variants: ${intersection.size}")

    if (intersection.isEmpty()) {
      logger.lifecycle("No resources are unused across all variants. Nothing to prune.")
      return
    }

    val intersectedEntries = intersection.map { UnusedResourceEntry.deserialize(it) }.toSet()

    // Re-collect resources from disk to get accurate DetectedResource objects
    // De-duplicate directories since multiple variants may share the same source sets
    val resDirs = resDirectories.files.map { it.toPath() }.distinct()
    val sourceDirs = sourceDirectories.files.map { it.toPath() }.distinct()

    val excludePatterns = compileExcludePatterns()
    val targetTypes = targetResourceTypes.get()
    val excludeTypes = excludeResourceTypes.get()

    val collector = CompositeResourceCollector.createDefault()
    val detector = CompositeUsageDetector.createDefault()
    val pruner = DefaultResourcePruner()

    if (isPreview) {
      previewResources(collector, detector, pruner, resDirs, sourceDirs, excludePatterns, targetTypes, excludeTypes, intersectedEntries)
    } else {
      pruneResources(collector, detector, pruner, resDirs, sourceDirs, excludePatterns, targetTypes, excludeTypes, intersectedEntries)
    }
  }

  private fun previewResources(
    collector: CompositeResourceCollector,
    detector: CompositeUsageDetector,
    pruner: DefaultResourcePruner,
    resDirs: List<java.nio.file.Path>,
    sourceDirs: List<java.nio.file.Path>,
    excludePatterns: List<Regex>,
    targetTypes: Set<String>,
    excludeTypes: Set<String>,
    intersectedEntries: Set<UnusedResourceEntry>,
  ) {
    val detectedResources = collector.collect(resDirs)
    val references = detector.detect(sourceDirs, resDirs)
    val analysis = pruner.analyze(detectedResources, references, excludePatterns, targetTypes, excludeTypes)

    // Filter to only resources in the intersection
    val toPreview = analysis.toBeRemoved.filter { resource ->
      UnusedResourceEntry(resource.type.typeName, resource.name) in intersectedEntries
    }

    logger.lifecycle("")
    logger.lifecycle("=== Aggregate Analysis Results ===")
    logger.lifecycle("Total resources: ${detectedResources.size}")
    logger.lifecycle("Resources to prune (unused across all variants): ${toPreview.size}")

    if (toPreview.isNotEmpty()) {
      logger.lifecycle("")
      logger.lifecycle("Unused resources:")
      toPreview
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

    val isCascade = cascadePrune.getOrElse(false)
    if (isCascade) {
      logger.lifecycle("")
      logger.lifecycle(
        "Note: Cascade preview is not supported. Run 'pruneResources' task with cascadePrune enabled to see cascade effects.",
      )
    }

    logger.lifecycle("")
    logger.lifecycle("Run 'pruneResources' task to remove unused resources.")
  }

  private fun pruneResources(
    collector: CompositeResourceCollector,
    detector: CompositeUsageDetector,
    pruner: DefaultResourcePruner,
    resDirs: List<java.nio.file.Path>,
    sourceDirs: List<java.nio.file.Path>,
    excludePatterns: List<Regex>,
    targetTypes: Set<String>,
    excludeTypes: Set<String>,
    intersectedEntries: Set<UnusedResourceEntry>,
  ) {
    val isCascade = cascadePrune.getOrElse(false)
    if (isCascade) {
      logger.lifecycle("Cascade pruning enabled (max $maxCascadeIterations iterations)")
    }

    var totalPrunedCount = 0
    var totalErrorCount = 0
    var iteration = 0
    var lastIterationPrunedCount = 0
    val allPrunedResources = mutableListOf<PrunedResource>()
    val allErrors = mutableListOf<PruneError>()

    do {
      iteration++
      if (isCascade && iteration > 1) {
        logger.lifecycle("")
        logger.lifecycle("=== Cascade iteration $iteration ===")
      }

      // Re-collect and re-detect on current state
      val detectedResources = collector.collect(resDirs)
      val references = detector.detect(sourceDirs, resDirs)
      val analysis = pruner.analyze(detectedResources, references, excludePatterns, targetTypes, excludeTypes)

      // On the first iteration, filter to only the intersection set.
      // On subsequent cascade iterations, all newly unused resources are safe to delete
      // because source code hasn't changed — they became unused due to prior deletions.
      val toRemove = if (iteration == 1) {
        analysis.toBeRemoved.filter { resource ->
          UnusedResourceEntry(resource.type.typeName, resource.name) in intersectedEntries
        }
      } else {
        analysis.toBeRemoved
      }

      if (toRemove.isEmpty()) {
        if (iteration == 1) {
          logger.lifecycle("No unused resources found. Nothing to prune.")
        } else {
          logger.lifecycle("No more unused resources found.")
        }
        break
      }

      logger.lifecycle("Found ${toRemove.size} unused resources. Pruning...")

      // Create a filtered analysis for execution
      val filteredAnalysis = PruneAnalysis(
        toBeRemoved = toRemove,
        toBePreserved = analysis.toBePreserved,
      )

      val report = pruner.execute(filteredAnalysis)

      lastIterationPrunedCount = report.prunedCount
      totalPrunedCount += report.prunedCount
      totalErrorCount += report.errorCount
      allPrunedResources.addAll(report.prunedResources)
      allErrors.addAll(report.errors)

      if (isCascade) {
        logger.lifecycle("Iteration $iteration: pruned ${report.prunedCount} resources")
      }
    } while (isCascade && iteration < maxCascadeIterations && lastIterationPrunedCount > 0)

    // Report results
    logger.lifecycle("")
    logger.lifecycle("=== Aggregate Pruning Results ===")
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
      logger.lifecycle("Aggregate pruning completed successfully!")
    } else {
      logger.warn("Aggregate pruning completed with $totalErrorCount errors.")
    }
  }
}
