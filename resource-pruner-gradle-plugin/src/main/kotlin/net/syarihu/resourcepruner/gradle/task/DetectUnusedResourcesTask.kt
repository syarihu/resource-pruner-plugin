package net.syarihu.resourcepruner.gradle.task

import net.syarihu.resourcepruner.collector.CompositeResourceCollector
import net.syarihu.resourcepruner.detector.CompositeUsageDetector
import net.syarihu.resourcepruner.gradle.model.DetectionResult
import net.syarihu.resourcepruner.gradle.model.UnusedResourceEntry
import net.syarihu.resourcepruner.pruner.DefaultResourcePruner
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Task for detecting unused resources without removing them.
 *
 * This task performs the detection phase only and writes the results
 * to a file for later aggregation by [AggregatePruneResourcesTask].
 */
abstract class DetectUnusedResourcesTask : BaseResourcePrunerTask() {
  /**
   * The variant name for this detection task.
   */
  @get:Input
  abstract val variantName: Property<String>

  /**
   * The output file where detection results are written.
   */
  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun detect() {
    val resDirs = resDirectories.files.map { it.toPath() }
    val sourceDirs = sourceDirectories.files.map { it.toPath() }

    logger.info("Detecting unused resources...")
    logger.info("Resource directories: ${resDirs.size}")
    logger.info("Source directories: ${sourceDirs.size}")

    val excludePatterns = compileExcludePatterns()
    val targetTypes = targetResourceTypes.get()
    val excludeTypes = excludeResourceTypes.get()

    val collector = CompositeResourceCollector.createDefault()
    val detector = CompositeUsageDetector.createDefault()
    val pruner = DefaultResourcePruner()

    // Collect resources
    val detectedResources = collector.collect(resDirs)
    logger.info("Detected ${detectedResources.size} resources")

    // Detect usage
    val references = detector.detect(sourceDirs, resDirs)
    logger.info("Found ${references.size} resource references")

    // Analyze
    val analysis = pruner.analyze(
      detectedResources,
      references,
      excludePatterns,
      targetTypes,
      excludeTypes,
    )

    logger.info("Found ${analysis.toBeRemoved.size} unused resources")

    // Serialize results
    val entries = analysis.toBeRemoved.map { resource ->
      UnusedResourceEntry(
        type = resource.type.typeName,
        name = resource.name,
      )
    }

    val outputFileValue = outputFile.get().asFile
    val variant = variantName.get()
    val result = DetectionResult(
      variantName = variant,
      unusedResources = entries,
    )
    result.writeTo(outputFileValue)

    logger.lifecycle("Detection complete: ${entries.size} unused resources found for variant '$variant'")
  }
}
