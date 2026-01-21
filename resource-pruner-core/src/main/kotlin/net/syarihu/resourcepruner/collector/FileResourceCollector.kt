package net.syarihu.resourcepruner.collector

import net.syarihu.resourcepruner.model.DetectedResource
import net.syarihu.resourcepruner.model.ResourceLocation
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.streams.toList

/**
 * Collects file-based resources from Android resource directories.
 *
 * This collector scans directories like drawable/, layout/, menu/, etc.
 * and extracts resource information from filenames.
 */
class FileResourceCollector : ResourceCollector {
  override fun collect(resDirectories: List<Path>): List<DetectedResource> {
    return resDirectories
      .filter { it.exists() && it.isDirectory() }
      .flatMap { collectFromResDirectory(it) }
  }

  private fun collectFromResDirectory(resDir: Path): List<DetectedResource> {
    return Files.list(resDir).use { stream ->
      stream
        .filter { it.isDirectory() }
        .toList()
        .flatMap { subDir ->
          val dirName = subDir.fileName.toString()
          val resourceType = ResourceType.fromDirectoryName(dirName)
          if (resourceType != null) {
            collectFromResourceTypeDirectory(subDir, resourceType, dirName)
          } else {
            emptyList()
          }
        }
    }
  }

  private fun collectFromResourceTypeDirectory(
    dir: Path,
    resourceType: ResourceType.File,
    dirName: String,
  ): List<DetectedResource> {
    val qualifiers = extractQualifiers(dirName)

    return Files.list(dir).use { stream ->
      stream
        .filter { it.isRegularFile() }
        .filter { isResourceFile(it) }
        .toList()
        .map { file ->
          DetectedResource(
            name = extractResourceName(file),
            type = resourceType,
            location = ResourceLocation.FileLocation(file),
            qualifiers = qualifiers,
          )
        }
    }
  }

  /**
   * Extracts the resource name from a file.
   *
   * Handles 9-patch images by removing the .9 suffix.
   * For example:
   * - "icon.png" -> "icon"
   * - "icon.9.png" -> "icon" (not "icon.9")
   * - "bg_button.xml" -> "bg_button"
   */
  private fun extractResourceName(file: Path): String {
    val nameWithoutExt = file.nameWithoutExtension
    // Handle 9-patch images: remove the .9 suffix
    return if (nameWithoutExt.endsWith(".9")) {
      nameWithoutExt.dropLast(2)
    } else {
      nameWithoutExt
    }
  }

  /**
   * Extracts qualifiers from a directory name.
   *
   * For example:
   * - "drawable" -> emptySet()
   * - "drawable-hdpi" -> setOf("hdpi")
   * - "layout-land-night" -> setOf("land", "night")
   */
  private fun extractQualifiers(dirName: String): Set<String> {
    val parts = dirName.split('-')
    return if (parts.size > 1) {
      parts.drop(1).toSet()
    } else {
      emptySet()
    }
  }

  /**
   * Checks if the file is a valid resource file.
   *
   * Resource files are typically XML or image files.
   * Excludes hidden files and certain system files.
   */
  private fun isResourceFile(file: Path): Boolean {
    val fileName = file.fileName.toString()

    // Skip hidden files
    if (fileName.startsWith(".")) {
      return false
    }

    // Skip system files
    if (fileName == "Thumbs.db" || fileName == ".DS_Store") {
      return false
    }

    // Accept common resource file extensions
    val extension = file.extension.lowercase()
    return extension in VALID_EXTENSIONS
  }

  companion object {
    // Note: 9-patch images (.9.png) have extension "png".
    // The .9 suffix is removed by extractResourceName() to get the correct resource name.
    private val VALID_EXTENSIONS = setOf(
      "xml",
      "png",
      "jpg",
      "jpeg",
      "gif",
      "webp",
    )
  }
}
