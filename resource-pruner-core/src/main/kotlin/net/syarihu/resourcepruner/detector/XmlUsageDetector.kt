package net.syarihu.resourcepruner.detector

import net.syarihu.resourcepruner.model.ReferenceLocation
import net.syarihu.resourcepruner.model.ReferencePattern
import net.syarihu.resourcepruner.model.ResourceReference
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.toList

/**
 * Detects resource usage in XML files.
 *
 * This detector finds references like:
 * - @drawable/icon
 * - @string/app_name
 * - @color/colorPrimary
 * - @style/AppTheme
 * - @layout/activity_main
 *
 * It scans layout files, manifest, and other XML resources.
 */
class XmlUsageDetector : UsageDetector {
  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    val references = mutableSetOf<ResourceReference>()

    // Scan res directories for XML files
    resDirectories
      .filter { it.exists() && it.isDirectory() }
      .forEach { resDir ->
        references.addAll(detectInResDirectory(resDir))
      }

    // Also scan source roots for AndroidManifest.xml and other XML files
    sourceRoots
      .filter { it.exists() }
      .forEach { sourceRoot ->
        if (sourceRoot.isDirectory()) {
          references.addAll(detectManifestAndOtherXml(sourceRoot))
        } else if (sourceRoot.isRegularFile() && sourceRoot.extension.lowercase() == "xml") {
          // Handle individual XML files (e.g., AndroidManifest.xml passed directly)
          references.addAll(detectInXmlFile(sourceRoot))
        }
      }

    return references
  }

  private fun detectInResDirectory(resDir: Path): Set<ResourceReference> {
    return Files.walk(resDir).use { stream ->
      stream
        .filter { it.isRegularFile() }
        .filter { it.extension.lowercase() == "xml" }
        .toList()
        .flatMap { detectInXmlFile(it) }
        .toSet()
    }
  }

  private fun detectManifestAndOtherXml(sourceRoot: Path): Set<ResourceReference> {
    return Files.walk(sourceRoot).use { stream ->
      stream
        .filter { it.isRegularFile() }
        .filter { it.extension.lowercase() == "xml" }
        .filter { !isInBuildDirectory(it) }
        .toList()
        .flatMap { detectInXmlFile(it) }
        .toSet()
    }
  }

  private fun isInBuildDirectory(path: Path): Boolean {
    return path.toString().contains("/build/") || path.toString().contains("\\build\\")
  }

  private fun detectInXmlFile(file: Path): List<ResourceReference> {
    val content = file.readText()
    val lines = content.lines()
    val references = mutableListOf<ResourceReference>()

    lines.forEachIndexed { lineIndex, line ->
      val lineNumber = lineIndex + 1

      // Skip XML comments
      if (line.trim().startsWith("<!--")) {
        return@forEachIndexed
      }

      // Skip tools: namespace attributes (design-time only)
      val lineWithoutTools = removeToolsAttributes(line)

      // Find @type/name references
      RESOURCE_REFERENCE_PATTERN.findAll(lineWithoutTools).forEach { match ->
        val typeName = match.groupValues[1]
        val resourceName = match.groupValues[2]
        val resourceType = mapXmlTypeToResourceType(typeName)

        if (resourceType != null) {
          val column = match.range.first + 1
          references.add(
            ResourceReference(
              resourceName = resourceName,
              resourceType = resourceType,
              location = ReferenceLocation(
                filePath = file,
                line = lineNumber,
                column = column,
                pattern = ReferencePattern.XML_REFERENCE,
              ),
            ),
          )
        }
      }

      // Find style parent references (parent="@style/xxx" or parent="Theme.xxx")
      STYLE_PARENT_PATTERN.findAll(lineWithoutTools).forEach { match ->
        val parentName = match.groupValues[1]
        // Only add if it's a local style reference (not a framework style)
        if (!parentName.startsWith("android:") && !parentName.startsWith("Theme.") && !parentName.contains(".")) {
          val column = match.range.first + 1
          references.add(
            ResourceReference(
              resourceName = parentName,
              resourceType = ResourceType.Value.Style,
              location = ReferenceLocation(
                filePath = file,
                line = lineNumber,
                column = column,
                pattern = ReferencePattern.XML_REFERENCE,
              ),
            ),
          )
        }
      }

      // Find implicit style parent references from dot notation in style names
      // e.g., <style name="TextStyle.Body"> implicitly inherits from TextStyle
      // e.g., <style name="TextStyle.Body.Bold"> implicitly inherits from TextStyle.Body
      STYLE_NAME_PATTERN.findAll(lineWithoutTools).forEach { match ->
        val styleName = match.groupValues[1]
        // Only process if the style name contains a dot (indicating inheritance)
        if (styleName.contains(".") && !styleName.startsWith("android:") && !styleName.startsWith("Theme.")) {
          val column = match.range.first + 1
          // Extract all parent style names from the dot notation
          val parentStyleNames = extractParentStyleNames(styleName)
          parentStyleNames.forEach { parentName ->
            references.add(
              ResourceReference(
                resourceName = parentName,
                resourceType = ResourceType.Value.Style,
                location = ReferenceLocation(
                  filePath = file,
                  line = lineNumber,
                  column = column,
                  pattern = ReferencePattern.XML_REFERENCE,
                ),
              ),
            )
          }
        }
      }
    }

    return references
  }

  /**
   * Removes tools: namespace attributes from a line.
   * These are design-time only and shouldn't be counted as real references.
   */
  private fun removeToolsAttributes(line: String): String {
    return TOOLS_ATTRIBUTE_PATTERN.replace(line, "")
  }

  /**
   * Extracts all parent style names from a dot-notation style name.
   *
   * For example:
   * - "TextStyle.Body" returns ["TextStyle"]
   * - "TextStyle.Body.Bold" returns ["TextStyle.Body", "TextStyle"]
   * - "Parent.Child.GrandChild" returns ["Parent.Child", "Parent"]
   */
  private fun extractParentStyleNames(styleName: String): List<String> {
    val parts = styleName.split(".")
    if (parts.size < 2) return emptyList()

    val parents = mutableListOf<String>()
    // Build parent names from the longest to the shortest
    // e.g., for "A.B.C", we want "A.B" and "A"
    for (i in parts.size - 1 downTo 1) {
      val parentName = parts.subList(0, i).joinToString(".")
      parents.add(parentName)
    }
    return parents
  }

  /**
   * Maps XML resource type names to ResourceType.
   */
  private fun mapXmlTypeToResourceType(typeName: String): ResourceType? {
    return when (typeName) {
      "drawable" -> ResourceType.File.Drawable
      "layout" -> ResourceType.File.Layout
      "menu" -> ResourceType.File.Menu
      "mipmap" -> ResourceType.File.Mipmap
      "animator" -> ResourceType.File.Animator
      "anim" -> ResourceType.File.Anim
      "color" -> ResourceType.Value.Color // Could be file or value, default to value
      "string" -> ResourceType.Value.StringRes
      "dimen" -> ResourceType.Value.Dimen
      "style" -> ResourceType.Value.Style
      "bool" -> ResourceType.Value.Bool
      "integer" -> ResourceType.Value.Integer
      "array" -> ResourceType.Value.Array
      "attr" -> ResourceType.Value.Attr
      "plurals" -> ResourceType.Value.Plurals
      "id" -> null // IDs are not resource files we track
      "raw" -> null // Raw resources not tracked
      "font" -> null // Font resources not tracked for now
      "xml" -> null // XML resources not tracked for now
      else -> null
    }
  }

  companion object {
    /**
     * Pattern to match XML resource references.
     *
     * Matches patterns like:
     * - @drawable/icon
     * - @string/app_name
     * - @+id/button (the + is optional)
     * - @style/Theme.Example (style names can contain dots)
     *
     * Group 1: Resource type
     * Group 2: Resource name (may contain dots for styles/themes)
     */
    private val RESOURCE_REFERENCE_PATTERN = Regex(
      """@\+?(\w+)/([\w.]+)""",
    )

    /**
     * Pattern to match style parent attribute.
     *
     * Matches: parent="StyleName"
     * Group 1: Parent style name
     */
    private val STYLE_PARENT_PATTERN = Regex(
      """parent\s*=\s*"([^"@]+)"""",
    )

    /**
     * Pattern to match style name attribute in style definitions.
     *
     * Matches: <style name="StyleName.Child">
     * Group 1: Style name
     */
    private val STYLE_NAME_PATTERN = Regex(
      """<style\s+[^>]*name\s*=\s*"([^"]+)"""",
    )

    /**
     * Pattern to match tools: namespace attributes.
     */
    private val TOOLS_ATTRIBUTE_PATTERN = Regex(
      """tools:\w+\s*=\s*"[^"]*"""",
    )
  }
}
