package net.syarihu.resourcepruner.gradle.model

import java.io.File

/**
 * Represents an unused resource entry identified during detection.
 */
data class UnusedResourceEntry(
  val type: String,
  val name: String,
) {
  /**
   * Serializes to "type/name" format.
   */
  fun serialize(): String = "$type/$name"

  companion object {
    /**
     * Deserializes from "type/name" format.
     */
    fun deserialize(line: String): UnusedResourceEntry {
      val parts = line.split("/", limit = 2)
      require(parts.size == 2) { "Invalid format: $line" }
      return UnusedResourceEntry(type = parts[0], name = parts[1])
    }
  }
}

/**
 * Represents the detection result for a single variant.
 */
data class DetectionResult(
  val variantName: String,
  val unusedResources: List<UnusedResourceEntry>,
) {
  /**
   * Writes the detection result to a file.
   *
   * Format:
   * ```
   * # variant: debug
   * drawable/ic_unused_icon
   * string/unused_string
   * ```
   */
  fun writeTo(file: File) {
    file.parentFile?.mkdirs()
    val lines = buildList {
      add("# variant: $variantName")
      unusedResources.forEach { add(it.serialize()) }
    }
    file.writeText(lines.joinToString("\n"))
  }

  companion object {
    /**
     * Reads a detection result from a file.
     */
    fun readFrom(file: File): DetectionResult {
      val lines = file.readLines().filter { it.isNotBlank() }
      val variantLine = lines.firstOrNull { it.startsWith("# variant: ") }
      val variantName = variantLine?.removePrefix("# variant: ")?.trim()
        ?: throw IllegalStateException(
          "Missing variant header in detection result file: ${file.path}. " +
            "Expected line starting with '# variant: '.",
        )
      val entries = lines
        .filter { !it.startsWith("#") }
        .map { UnusedResourceEntry.deserialize(it) }
      return DetectionResult(
        variantName = variantName,
        unusedResources = entries,
      )
    }
  }
}
