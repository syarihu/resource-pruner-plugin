package net.syarihu.resourcepruner.pruner

import net.syarihu.resourcepruner.model.DetectedResource
import net.syarihu.resourcepruner.model.PreservedResource
import net.syarihu.resourcepruner.model.PruneError
import net.syarihu.resourcepruner.model.PruneReport
import net.syarihu.resourcepruner.model.PrunedResource
import net.syarihu.resourcepruner.model.ResourceLocation
import net.syarihu.resourcepruner.model.ResourceReference

/**
 * Default implementation of [ResourcePruner].
 *
 * This class handles the analysis and execution of resource pruning,
 * carefully removing unused resources while preserving those that
 * match exclusion patterns or are still in use.
 */
class DefaultResourcePruner(
  private val xmlEditor: XmlEditor = XmlEditor(),
) : ResourcePruner {
  override fun analyze(
    detectedResources: List<DetectedResource>,
    references: Set<ResourceReference>,
    excludePatterns: List<Regex>,
    targetResourceTypes: Set<String>,
    excludeResourceTypes: Set<String>,
  ): PruneAnalysis {
    // Build a set of referenced resource names for quick lookup
    val referencedResources = references
      .map { ref -> ResourceKey(ref.resourceName, ref.resourceType.typeName) }
      .toSet()

    val toBeRemoved = mutableListOf<DetectedResource>()
    val toBePreserved = mutableListOf<DetectedResource>()

    for (resource in detectedResources) {
      val resourceKey = ResourceKey(resource.name, resource.type.typeName)
      val typeName = resource.type.typeName

      when {
        // Check if the resource type is excluded
        excludeResourceTypes.contains(typeName) -> {
          toBePreserved.add(resource)
        }

        // Check if the resource type is not in target types (if target types are specified)
        targetResourceTypes.isNotEmpty() && !targetResourceTypes.contains(typeName) -> {
          toBePreserved.add(resource)
        }

        // Check if the resource matches any exclude pattern
        excludePatterns.any { pattern -> pattern.matches(resource.name) } -> {
          toBePreserved.add(resource)
        }

        // Check if the resource is referenced
        referencedResources.contains(resourceKey) -> {
          toBePreserved.add(resource)
        }

        // Also check without type (some references might use different types)
        references.any { it.resourceName == resource.name } -> {
          toBePreserved.add(resource)
        }

        else -> {
          toBeRemoved.add(resource)
        }
      }
    }

    return PruneAnalysis(
      toBeRemoved = toBeRemoved,
      toBePreserved = toBePreserved,
    )
  }

  override fun execute(analysis: PruneAnalysis): PruneReport {
    val prunedResources = mutableListOf<PrunedResource>()
    val preservedResources = mutableListOf<PreservedResource>()
    val errors = mutableListOf<PruneError>()

    // Group value resources by file for efficient batch processing
    val valueResourcesByFile = analysis.toBeRemoved
      .filter { it.location is ResourceLocation.ValueLocation }
      .groupBy { (it.location as ResourceLocation.ValueLocation).filePath }

    val fileResources = analysis.toBeRemoved
      .filter { it.location is ResourceLocation.FileLocation }

    // Process file resources (simple deletion)
    for (resource in fileResources) {
      val result = xmlEditor.removeFileResource(resource)
      if (result.isSuccess) {
        prunedResources.add(
          PrunedResource(
            resource = resource,
            reason = "No references found",
          ),
        )
      } else {
        errors.add(
          PruneError(
            resource = resource,
            message = result.exceptionOrNull()?.message ?: "Unknown error",
          ),
        )
      }
    }

    // Process value resources by file
    for ((_, resources) in valueResourcesByFile) {
      val result = xmlEditor.removeValueResources(resources)
      if (result.isSuccess) {
        for (resource in resources) {
          prunedResources.add(
            PrunedResource(
              resource = resource,
              reason = "No references found",
            ),
          )
        }
      } else {
        for (resource in resources) {
          errors.add(
            PruneError(
              resource = resource,
              message = result.exceptionOrNull()?.message ?: "Unknown error",
            ),
          )
        }
      }
    }

    // Record preserved resources
    for (resource in analysis.toBePreserved) {
      preservedResources.add(
        PreservedResource(
          resource = resource,
          reason = "Resource is referenced or matches exclude pattern",
        ),
      )
    }

    return PruneReport(
      prunedResources = prunedResources,
      preservedResources = preservedResources,
      errors = errors,
    )
  }

  /**
   * Key for identifying a resource by name and type.
   */
  private data class ResourceKey(
    val name: String,
    val typeName: String,
  )
}
