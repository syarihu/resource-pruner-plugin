package net.syarihu.resourcepruner.detector

import net.syarihu.resourcepruner.model.ReferenceLocation
import net.syarihu.resourcepruner.model.ReferencePattern
import net.syarihu.resourcepruner.model.ResourceReference
import net.syarihu.resourcepruner.model.ResourceType
import net.syarihu.resourcepruner.parser.SourceTokenizer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.streams.toList

/**
 * Detects resource usage in Kotlin and Java source files.
 *
 * This detector finds references like:
 * - R.drawable.icon
 * - R.string.app_name
 * - R.layout.activity_main
 *
 * It also handles import aliases like:
 * - import com.example.R as MyR → MyR.string.app_name
 * - import com.example.R as R_resource → R_resource.drawable.icon
 *
 * It also handles Compose resource functions like:
 * - stringResource(R.string.xxx)
 * - painterResource(R.drawable.xxx)
 *
 * Note: This detector skips generated directories (Paraphrase, ViewBinding) to avoid
 * marking resources as used when they're only referenced in generated code.
 * Actual usage is detected by dedicated detectors (ParaphraseUsageDetector,
 * ViewBindingUsageDetector).
 */
class KotlinUsageDetector : UsageDetector {
  private val tokenizer = SourceTokenizer()

  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    return sourceRoots
      .filter { it.exists() && it.isDirectory() }
      .filter { !isGeneratedDirectory(it) }
      .flatMap { detectInDirectory(it) }
      .toSet()
  }

  private fun isGeneratedDirectory(path: Path): Boolean {
    return ParaphraseUsageDetector.isParaphraseGeneratedDirectory(path) ||
      ViewBindingUsageDetector.isViewBindingGeneratedDirectory(path)
  }

  private fun detectInDirectory(directory: Path): List<ResourceReference> {
    return Files.walk(directory).use { stream ->
      stream
        .filter { it.isRegularFile() }
        .filter { it.extension in SUPPORTED_EXTENSIONS }
        .toList()
        .flatMap { detectInFile(it) }
    }
  }

  private fun detectInFile(file: Path): List<ResourceReference> {
    val content = file.readText()
    val cleanedContent = tokenizer.removeCommentsAndStrings(content)
    val cleanedLines = cleanedContent.lines()

    // Extract R class aliases from import statements
    val rClassAliases = extractRClassAliases(content)
    val pattern = buildRClassPattern(rClassAliases)

    val references = mutableListOf<ResourceReference>()

    cleanedLines.forEachIndexed { lineIndex, cleanedLine ->
      val lineNumber = lineIndex + 1

      pattern.findAll(cleanedLine).forEach { match ->
        val typeName = match.groupValues[1]
        val resourceName = match.groupValues[2]
        val resourceType = ResourceType.fromTypeName(typeName)

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
                pattern = if (file.extension == "java") {
                  ReferencePattern.JAVA_R_CLASS
                } else {
                  ReferencePattern.KOTLIN_R_CLASS
                },
              ),
            ),
          )
        }
      }

      // Find R.styleable.StyleableName_attrName references
      // Extract the attr name (after the last underscore that follows the styleable name)
      // e.g., R.styleable.CustomView_customBackground -> customBackground
      R_STYLEABLE_PATTERN.findAll(cleanedLine).forEach { match ->
        val fullName = match.groupValues[1] // e.g., "CustomView_customBackground"
        val attrName = extractAttrNameFromStyleable(fullName)
        if (attrName != null) {
          val column = match.range.first + 1
          references.add(
            ResourceReference(
              resourceName = attrName,
              resourceType = ResourceType.Value.Attr,
              location = ReferenceLocation(
                filePath = file,
                line = lineNumber,
                column = column,
                pattern = if (file.extension == "java") {
                  ReferencePattern.JAVA_R_CLASS
                } else {
                  ReferencePattern.KOTLIN_R_CLASS
                },
              ),
            ),
          )
        }
      }
    }

    return references
  }

  /**
   * Extracts the attr name from a styleable reference.
   *
   * R.styleable references have the format: StyleableName_attrName
   * e.g., CustomView_customBackground -> customBackground
   *
   * @return The attr name, or null if the format is invalid
   */
  private fun extractAttrNameFromStyleable(fullName: String): String? {
    val underscoreIndex = fullName.indexOf('_')
    if (underscoreIndex == -1 || underscoreIndex == fullName.length - 1) {
      return null
    }
    return fullName.substring(underscoreIndex + 1)
  }

  /**
   * Extracts R class aliases from import statements.
   *
   * Matches patterns like:
   * - import com.example.R as MyR
   * - import com.example.resources.R as R_resource
   *
   * @return Set of alias names (e.g., {"MyR", "R_resource"})
   */
  private fun extractRClassAliases(content: String): Set<String> {
    return R_CLASS_IMPORT_ALIAS_PATTERN.findAll(content)
      .map { it.groupValues[1] }
      .toSet()
  }

  /**
   * Builds a regex pattern that matches R class references including aliases.
   *
   * @param aliases Set of alias names to include in the pattern
   * @return Regex that matches R.type.name and Alias.type.name
   */
  private fun buildRClassPattern(aliases: Set<String>): Regex {
    if (aliases.isEmpty()) {
      return R_CLASS_PATTERN
    }
    // Escape alias names for regex safety and join with |
    val escapedAliases = aliases.map { Regex.escape(it) }
    val allIdentifiers = listOf("R") + escapedAliases
    val identifierPattern = allIdentifiers.joinToString("|")
    return Regex("""(?:$identifierPattern)\.(\w+)\.(\w+)""")
  }

  companion object {
    private val SUPPORTED_EXTENSIONS = setOf("kt", "java")

    /**
     * Pattern to match R class resource references.
     *
     * Matches patterns like:
     * - R.drawable.icon
     * - R.string.app_name
     * - R.layout.activity_main
     *
     * Group 1: Resource type (drawable, string, layout, etc.)
     * Group 2: Resource name
     */
    private val R_CLASS_PATTERN = Regex(
      """R\.(\w+)\.(\w+)""",
    )

    /**
     * Pattern to match R class import aliases.
     *
     * Matches patterns like:
     * - import com.example.R as MyR
     * - import com.example.resources.R as R_resource
     *
     * Group 1: Alias name
     */
    private val R_CLASS_IMPORT_ALIAS_PATTERN = Regex(
      """import\s+[\w.]+\.R\s+as\s+(\w+)""",
    )

    /**
     * Pattern to match R.styleable references.
     *
     * Matches patterns like:
     * - R.styleable.CustomView_customBackground
     * - R.styleable.MyView_customAttr
     *
     * Group 1: Full styleable name (StyleableName_attrName)
     */
    private val R_STYLEABLE_PATTERN = Regex(
      """R\.styleable\.(\w+)""",
    )
  }
}
