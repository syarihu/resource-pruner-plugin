package net.syarihu.resourcepruner.collector

import net.syarihu.resourcepruner.model.DetectedResource
import java.nio.file.Path

/**
 * A composite resource collector that combines multiple collectors.
 *
 * This collector delegates to multiple underlying collectors and
 * combines their results.
 *
 * @property collectors The list of collectors to delegate to
 */
class CompositeResourceCollector(
  private val collectors: List<ResourceCollector>,
) : ResourceCollector {
  override fun collect(resDirectories: List<Path>): List<DetectedResource> {
    return collectors.flatMap { it.collect(resDirectories) }
  }

  companion object {
    /**
     * Creates a default composite collector with all standard collectors.
     */
    fun createDefault(): CompositeResourceCollector {
      return CompositeResourceCollector(
        listOf(
          FileResourceCollector(),
          ValueResourceCollector(),
        ),
      )
    }
  }
}
