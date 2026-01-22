package net.syarihu.resourcepruner.pruner

import net.syarihu.resourcepruner.model.DetectedResource
import net.syarihu.resourcepruner.model.PruneReport
import net.syarihu.resourcepruner.model.ResourceReference

/**
 * Interface for pruning unused resources.
 */
interface ResourcePruner {
  /**
   * Analyzes detected resources against references to determine what should be pruned.
   *
   * @param detectedResources All resources detected in the project
   * @param references All resource references found in the codebase
   * @param excludePatterns Patterns for resources to exclude from pruning
   * @param targetResourceTypes Resource types to target (empty = all types)
   * @param excludeResourceTypes Resource types to exclude from pruning
   * @return Analysis result containing resources to remove and preserve
   */
  fun analyze(
    detectedResources: List<DetectedResource>,
    references: Set<ResourceReference>,
    excludePatterns: List<Regex>,
    targetResourceTypes: Set<String> = emptySet(),
    excludeResourceTypes: Set<String> = emptySet(),
  ): PruneAnalysis

  /**
   * Executes the pruning operation based on the analysis.
   *
   * @param analysis The analysis result from [analyze]
   * @return Report of the pruning operation
   */
  fun execute(analysis: PruneAnalysis): PruneReport
}
