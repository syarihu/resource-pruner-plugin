package net.syarihu.resourcepruner.pruner

import net.syarihu.resourcepruner.model.DetectedResource
import net.syarihu.resourcepruner.model.ResourceLocation
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

/**
 * Editor for removing resources from the project.
 *
 * This class handles the actual file operations for removing resources,
 * preserving file formatting (indentation, etc.) when editing XML files.
 */
class XmlEditor {
  /**
   * Removes a file-based resource by deleting the file.
   *
   * @param resource The resource to remove
   * @return Result indicating success or failure
   */
  fun removeFileResource(resource: DetectedResource): Result<Unit> {
    return runCatching {
      val location = resource.location as? ResourceLocation.FileLocation
        ?: return Result.failure(IllegalArgumentException("Expected FileLocation for file resource"))

      val deleted = location.filePath.deleteIfExists()
      if (!deleted) {
        throw IllegalStateException("File does not exist: ${location.filePath}")
      }
    }
  }

  /**
   * Removes consecutive blank lines, leaving at most one blank line between elements.
   */
  private fun removeConsecutiveBlankLines(lines: List<String>): List<String> {
    val result = mutableListOf<String>()
    var previousWasBlank = false

    for (line in lines) {
      val isBlank = line.isBlank()

      if (isBlank) {
        if (!previousWasBlank) {
          result.add(line)
        }
        previousWasBlank = true
      } else {
        result.add(line)
        previousWasBlank = false
      }
    }

    return result
  }

  /**
   * Removes multiple resources from the same file efficiently.
   *
   * This method batches removals to avoid repeated file I/O.
   *
   * @param resources List of resources to remove (must all be from the same file)
   * @return Result indicating success or failure
   */
  fun removeValueResources(resources: List<DetectedResource>): Result<Unit> {
    if (resources.isEmpty()) {
      return Result.success(Unit)
    }

    return runCatching {
      // Verify all resources are from the same file
      val locations = resources.map {
        it.location as? ResourceLocation.ValueLocation
          ?: throw IllegalArgumentException("Expected ValueLocation for value resource")
      }

      val filePath = locations.first().filePath
      if (!locations.all { it.filePath == filePath }) {
        throw IllegalArgumentException("All resources must be from the same file")
      }

      val lines = filePath.readLines().toMutableList()

      // Sort by line number in descending order to remove from bottom to top
      val sortedLocations = locations.sortedByDescending { it.startLine }

      for (location in sortedLocations) {
        if (location.startLine < 1 || location.endLine > lines.size) {
          throw IllegalStateException(
            "Invalid line range: ${location.startLine}-${location.endLine} for file with ${lines.size} lines",
          )
        }

        val startIndex = location.startLine - 1
        val endIndex = location.endLine - 1

        for (i in endIndex downTo startIndex) {
          lines.removeAt(i)
        }
      }

      // Remove consecutive blank lines
      val cleanedLines = removeConsecutiveBlankLines(lines)

      // Check if the file is now essentially empty (only has resources tag)
      if (isEmptyResourcesFile(cleanedLines)) {
        // Delete the file entirely
        Files.deleteIfExists(filePath)
      } else {
        filePath.writeLines(cleanedLines)
      }
    }
  }

  /**
   * Checks if the file only contains an empty resources element.
   */
  private fun isEmptyResourcesFile(lines: List<String>): Boolean {
    val nonBlankLines = lines.filter { it.isNotBlank() }

    // Check if only XML declaration and empty resources tag remain
    if (nonBlankLines.size <= 3) {
      val content = nonBlankLines.joinToString("\n")
      return content.contains("<resources") &&
        (content.contains("</resources>") || content.contains("/>")) &&
        !content.contains("<string") &&
        !content.contains("<color") &&
        !content.contains("<dimen") &&
        !content.contains("<style") &&
        !content.contains("<bool") &&
        !content.contains("<integer") &&
        !content.contains("<array") &&
        !content.contains("<attr") &&
        !content.contains("<plurals")
    }

    return false
  }
}
