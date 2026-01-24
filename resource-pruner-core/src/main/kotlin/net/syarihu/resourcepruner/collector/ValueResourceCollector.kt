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
import kotlin.io.path.readText

/**
 * Collects value-based resources from Android resource directories.
 *
 * This collector scans values/ directories and extracts resource elements
 * like <string>, <color>, <dimen>, etc. from XML files.
 */
class ValueResourceCollector : ResourceCollector {
  override fun collect(resDirectories: List<Path>): List<DetectedResource> {
    return resDirectories
      .filter { it.exists() && it.isDirectory() }
      .flatMap { collectFromResDirectory(it) }
  }

  private fun collectFromResDirectory(resDir: Path): List<DetectedResource> {
    return Files.list(resDir).use { stream ->
      stream
        .filter { it.isDirectory() }
        .filter { it.fileName.toString().startsWith("values") }
        .toList()
        .flatMap { valuesDir ->
          val qualifiers = extractQualifiers(valuesDir.fileName.toString())
          collectFromValuesDirectory(valuesDir, qualifiers)
        }
    }
  }

  private fun collectFromValuesDirectory(
    valuesDir: Path,
    qualifiers: Set<String>,
  ): List<DetectedResource> {
    return Files.list(valuesDir).use { stream ->
      stream
        .filter { it.isRegularFile() }
        .filter { it.extension.lowercase() == "xml" }
        .toList()
        .flatMap { xmlFile ->
          parseXmlFile(xmlFile, qualifiers)
        }
    }
  }

  /**
   * Parses an XML file and extracts value resources.
   *
   * Uses a simple line-based approach to preserve line numbers
   * for later removal operations.
   */
  private fun parseXmlFile(
    xmlFile: Path,
    qualifiers: Set<String>,
  ): List<DetectedResource> {
    val content = xmlFile.readText()
    val lines = content.lines()
    val resources = mutableListOf<DetectedResource>()

    var currentElement: ElementBuilder? = null

    lines.forEachIndexed { index, line ->
      val lineNumber = index + 1 // 1-indexed

      if (currentElement == null) {
        // Look for element start
        val elementStart = parseElementStart(line)
        if (elementStart != null) {
          val (tagName, name, itemType) = elementStart

          if (isSelfClosing(line)) {
            // Single-line element
            val resourceType = getResourceTypeForElement(tagName, itemType)
            if (resourceType != null && name != null) {
              resources.add(
                DetectedResource(
                  name = name,
                  type = resourceType,
                  location = ResourceLocation.ValueLocation(
                    filePath = xmlFile,
                    startLine = lineNumber,
                    endLine = lineNumber,
                    elementXml = line,
                  ),
                  qualifiers = qualifiers,
                ),
              )
            }
          } else if (name != null) {
            // Multi-line element start
            currentElement = ElementBuilder(
              tagName = tagName,
              name = name,
              itemType = itemType,
              startLine = lineNumber,
            )
            currentElement.appendLine(line)

            // Check if element ends on the same line (but not self-closing)
            if (isElementEnd(line, tagName) && !isSelfClosing(line)) {
              currentElement.endLine = lineNumber
              val resource = currentElement.build(xmlFile, qualifiers)
              if (resource != null) {
                resources.add(resource)
              }
              currentElement = null
            }
          }
        }
      } else {
        // Continue building multi-line element
        currentElement.appendLine(line)

        if (isElementEnd(line, currentElement.tagName)) {
          currentElement.endLine = lineNumber
          val resource = currentElement.build(xmlFile, qualifiers)
          if (resource != null) {
            resources.add(resource)
          }
          currentElement = null
        }
      }
    }

    return resources
  }

  /**
   * Parses the start of an XML element and extracts tag name, resource name, and optional type.
   *
   * @return ParsedElement or null if not a valid resource element
   */
  private fun parseElementStart(line: String): ParsedElement? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("<") ||
      trimmed.startsWith("</") ||
      trimmed.startsWith("<?") ||
      trimmed.startsWith("<!--")
    ) {
      return null
    }

    // Extract tag name
    val tagMatch = TAG_NAME_PATTERN.find(trimmed) ?: return null
    val tagName = tagMatch.groupValues[1]

    // Check if it's a supported resource tag or an item tag
    if (!SUPPORTED_TAGS.contains(tagName) && tagName != "item") {
      return null
    }

    // Extract name attribute
    val nameMatch = NAME_ATTRIBUTE_PATTERN.find(trimmed)
    val name = nameMatch?.groupValues?.get(1)

    // For item tags, extract the type attribute
    val itemType = if (tagName == "item") {
      TYPE_ATTRIBUTE_PATTERN.find(trimmed)?.groupValues?.get(1)
    } else {
      null
    }

    // Skip item tags without a type attribute (they are style items, not resource definitions)
    if (tagName == "item" && itemType == null) {
      return null
    }

    return ParsedElement(tagName, name, itemType)
  }

  /**
   * Represents a parsed XML element.
   *
   * @property tagName The tag name of the element.
   * @property name The resource name (from name attribute).
   * @property itemType The type attribute value (only for `<item type="...">` elements).
   */
  private data class ParsedElement(
    val tagName: String,
    val name: String?,
    val itemType: String?,
  )

  private fun isSelfClosing(line: String): Boolean = line.trim().endsWith("/>")

  private fun isElementEnd(
    line: String,
    tagName: String,
  ): Boolean = line.contains("</$tagName>") || line.trim().endsWith("/>")

  private fun extractQualifiers(dirName: String): Set<String> {
    val parts = dirName.split('-')
    return if (parts.size > 1) {
      parts.drop(1).toSet()
    } else {
      emptySet()
    }
  }

  /**
   * Gets the resource type for an element.
   *
   * For regular tags (string, color, etc.), the type is determined by the tag name.
   * For <item> tags, the type is determined by the type attribute.
   */
  private fun getResourceTypeForElement(
    tagName: String,
    itemType: String?,
  ): ResourceType? {
    // For item tags, use the type attribute to determine the resource type
    if (tagName == "item" && itemType != null) {
      return getResourceTypeForItemType(itemType)
    }

    // For regular tags, use the tag name
    return when (tagName) {
      "string" -> ResourceType.Value.StringRes
      "color" -> ResourceType.Value.Color
      "dimen" -> ResourceType.Value.Dimen
      "style" -> ResourceType.Value.Style
      "bool" -> ResourceType.Value.Bool
      "integer" -> ResourceType.Value.Integer
      "array", "string-array", "integer-array" -> ResourceType.Value.Array
      "attr" -> ResourceType.Value.Attr
      "plurals" -> ResourceType.Value.Plurals
      "drawable" -> ResourceType.File.Drawable
      else -> null
    }
  }

  /**
   * Gets the resource type for an <item type="..."> element.
   *
   * This handles cases like:
   * - <item name="sample_icon" type="drawable">@drawable/sample_icon</item>
   * - <item name="sample_id" type="id"/>
   * - <item name="sample_color" type="color">#FF0000</item>
   */
  private fun getResourceTypeForItemType(itemType: String): ResourceType? {
    return when (itemType) {
      "drawable" -> ResourceType.File.Drawable
      "mipmap" -> ResourceType.File.Mipmap
      "raw" -> ResourceType.File.Raw
      "color" -> ResourceType.Value.Color
      "dimen" -> ResourceType.Value.Dimen
      "string" -> ResourceType.Value.StringRes
      "bool" -> ResourceType.Value.Bool
      "integer" -> ResourceType.Value.Integer
      "array" -> ResourceType.Value.Array
      "attr" -> ResourceType.Value.Attr
      "style" -> ResourceType.Value.Style
      "id" -> null // IDs are not tracked
      else -> null
    }
  }

  /**
   * Helper class to build multi-line elements.
   */
  private class ElementBuilder(
    val tagName: String,
    val name: String,
    val itemType: String?,
    val startLine: Int,
  ) {
    var endLine: Int = startLine
    private val lines = mutableListOf<String>()

    fun appendLine(line: String) {
      lines.add(line)
    }

    fun build(
      xmlFile: Path,
      qualifiers: Set<String>,
    ): DetectedResource? {
      val resourceType = getResourceType() ?: return null

      return DetectedResource(
        name = name,
        type = resourceType,
        location = ResourceLocation.ValueLocation(
          filePath = xmlFile,
          startLine = startLine,
          endLine = endLine,
          elementXml = lines.joinToString("\n"),
        ),
        qualifiers = qualifiers,
      )
    }

    private fun getResourceType(): ResourceType? {
      // For item tags, use the type attribute
      if (tagName == "item" && itemType != null) {
        return when (itemType) {
          "drawable" -> ResourceType.File.Drawable
          "mipmap" -> ResourceType.File.Mipmap
          "raw" -> ResourceType.File.Raw
          "color" -> ResourceType.Value.Color
          "dimen" -> ResourceType.Value.Dimen
          "string" -> ResourceType.Value.StringRes
          "bool" -> ResourceType.Value.Bool
          "integer" -> ResourceType.Value.Integer
          "array" -> ResourceType.Value.Array
          "attr" -> ResourceType.Value.Attr
          "style" -> ResourceType.Value.Style
          else -> null
        }
      }

      // For regular tags
      return when (tagName) {
        "string" -> ResourceType.Value.StringRes
        "color" -> ResourceType.Value.Color
        "dimen" -> ResourceType.Value.Dimen
        "style" -> ResourceType.Value.Style
        "bool" -> ResourceType.Value.Bool
        "integer" -> ResourceType.Value.Integer
        "array", "string-array", "integer-array" -> ResourceType.Value.Array
        "attr" -> ResourceType.Value.Attr
        "plurals" -> ResourceType.Value.Plurals
        "drawable" -> ResourceType.File.Drawable
        else -> null
      }
    }
  }

  companion object {
    private val TAG_NAME_PATTERN = Regex("""<(\w+(?:-\w+)?)[\s>]""")
    private val NAME_ATTRIBUTE_PATTERN = Regex("""name\s*=\s*"([^"]+)"""")
    private val TYPE_ATTRIBUTE_PATTERN = Regex("""type\s*=\s*"([^"]+)"""")

    private val SUPPORTED_TAGS = setOf(
      "string",
      "color",
      "dimen",
      "style",
      "bool",
      "integer",
      "array",
      "string-array",
      "integer-array",
      "attr",
      "plurals",
      "drawable",
    )
  }
}
