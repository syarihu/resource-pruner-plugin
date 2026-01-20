package net.syarihu.resourcepruner.collector

import net.syarihu.resourcepruner.model.DetectedResource
import java.nio.file.Path

/**
 * Interface for collecting resources from Android resource directories.
 */
interface ResourceCollector {
  /**
   * Collects resources from the given resource directories.
   *
   * @param resDirectories List of res/ directories to scan
   * @return List of detected resources
   */
  fun collect(resDirectories: List<Path>): List<DetectedResource>
}
