package net.syarihu.resourcepruner.model

/**
 * Report of the pruning operation.
 *
 * @property prunedResources Resources that were pruned (removed)
 * @property preservedResources Resources that were preserved (matched exclude patterns)
 * @property errors Errors that occurred during pruning
 */
data class PruneReport(
  val prunedResources: List<PrunedResource>,
  val preservedResources: List<PreservedResource>,
  val errors: List<PruneError>,
) {
  /**
   * Total number of resources that were pruned.
   */
  val prunedCount: Int get() = prunedResources.size

  /**
   * Total number of resources that were preserved.
   */
  val preservedCount: Int get() = preservedResources.size

  /**
   * Total number of errors.
   */
  val errorCount: Int get() = errors.size

  /**
   * Whether the pruning completed without errors.
   */
  val isSuccess: Boolean get() = errors.isEmpty()
}

/**
 * A resource that was pruned.
 *
 * @property resource The pruned resource
 * @property reason The reason for pruning (e.g., "No references found")
 */
data class PrunedResource(
  val resource: DetectedResource,
  val reason: String,
)

/**
 * A resource that was preserved (not pruned).
 *
 * @property resource The preserved resource
 * @property reason The reason for preservation (e.g., "Matched exclude pattern: ^ic_.*")
 */
data class PreservedResource(
  val resource: DetectedResource,
  val reason: String,
)

/**
 * An error that occurred during pruning.
 *
 * @property resource The resource that caused the error
 * @property message The error message
 */
data class PruneError(
  val resource: DetectedResource,
  val message: String,
)
