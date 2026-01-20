package net.syarihu.resourcepruner.pruner

import net.syarihu.resourcepruner.model.DetectedResource

/**
 * Result of analyzing resources for pruning.
 *
 * @property toBeRemoved Resources that should be removed (unused)
 * @property toBePreserved Resources that should be preserved (excluded or used)
 */
data class PruneAnalysis(
  val toBeRemoved: List<DetectedResource>,
  val toBePreserved: List<DetectedResource>,
)
