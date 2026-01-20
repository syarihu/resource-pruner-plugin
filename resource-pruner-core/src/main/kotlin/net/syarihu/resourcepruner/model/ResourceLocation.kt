package net.syarihu.resourcepruner.model

import java.nio.file.Path

/**
 * Represents the location of a resource in the project.
 */
sealed class ResourceLocation {
  /**
   * The file path where the resource is located.
   */
  abstract val filePath: Path

  /**
   * Location for file-based resources (the entire file is the resource).
   *
   * @property filePath The path to the resource file
   */
  data class FileLocation(
    override val filePath: Path,
  ) : ResourceLocation()

  /**
   * Location for value-based resources (a specific element within an XML file).
   *
   * @property filePath The path to the XML file containing the resource
   * @property startLine The starting line number of the resource element (1-indexed)
   * @property endLine The ending line number of the resource element (1-indexed)
   * @property elementXml The original XML content of the resource element
   */
  data class ValueLocation(
    override val filePath: Path,
    val startLine: Int,
    val endLine: Int,
    val elementXml: String,
  ) : ResourceLocation()
}
