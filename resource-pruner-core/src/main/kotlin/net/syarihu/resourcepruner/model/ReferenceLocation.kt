package net.syarihu.resourcepruner.model

import java.nio.file.Path

/**
 * Represents the location where a resource is referenced.
 *
 * @property filePath The file containing the reference
 * @property line The line number of the reference (1-indexed)
 * @property column The column number of the reference (1-indexed)
 * @property pattern The pattern used to reference the resource
 */
data class ReferenceLocation(
  val filePath: Path,
  val line: Int,
  val column: Int,
  val pattern: ReferencePattern,
)
