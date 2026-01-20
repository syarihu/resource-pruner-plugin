package net.syarihu.resourcepruner.detector

import net.syarihu.resourcepruner.model.ResourceReference
import java.nio.file.Path

/**
 * Interface for detecting resource usage in source code and resource files.
 */
interface UsageDetector {
  /**
   * Detects resource references in the given source and resource directories.
   *
   * @param sourceRoots List of source root directories (Kotlin/Java)
   * @param resDirectories List of res/ directories
   * @return Set of detected resource references
   */
  fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference>
}
