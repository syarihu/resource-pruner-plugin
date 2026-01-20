package net.syarihu.resourcepruner.detector

import net.syarihu.resourcepruner.model.ResourceReference
import java.nio.file.Path

/**
 * A composite usage detector that combines multiple detectors.
 *
 * This detector delegates to multiple underlying detectors and
 * combines their results.
 *
 * @property detectors The list of detectors to delegate to
 */
class CompositeUsageDetector(
  private val detectors: List<UsageDetector>,
) : UsageDetector {
  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    return detectors
      .flatMap { it.detect(sourceRoots, resDirectories) }
      .toSet()
  }

  companion object {
    /**
     * Creates a default composite detector with standard detectors.
     *
     * Includes:
     * - KotlinUsageDetector (for Kotlin/Java R class references)
     * - XmlUsageDetector (for XML resource references)
     * - ViewBindingUsageDetector (for ViewBinding references)
     * - ParaphraseUsageDetector (for Paraphrase FormattedResources usage)
     */
    fun createDefault(): CompositeUsageDetector {
      return CompositeUsageDetector(
        listOf(
          KotlinUsageDetector(),
          XmlUsageDetector(),
          ViewBindingUsageDetector(),
          ParaphraseUsageDetector(),
        ),
      )
    }
  }
}
