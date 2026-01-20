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
 * It also handles Compose resource functions like:
 * - stringResource(R.string.xxx)
 * - painterResource(R.drawable.xxx)
 *
 * Note: This detector skips Paraphrase-generated directories to avoid
 * marking all ICU-formatted strings as used. Actual Paraphrase usage
 * is detected by ParaphraseUsageDetector.
 */
class KotlinUsageDetector : UsageDetector {
  private val tokenizer = SourceTokenizer()

  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    return sourceRoots
      .filter { it.exists() && it.isDirectory() }
      .filter { !ParaphraseUsageDetector.isParaphraseGeneratedDirectory(it) }
      .flatMap { detectInDirectory(it) }
      .toSet()
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
    val lines = content.lines()
    val cleanedLines = cleanedContent.lines()

    val references = mutableListOf<ResourceReference>()

    cleanedLines.forEachIndexed { lineIndex, cleanedLine ->
      val lineNumber = lineIndex + 1

      R_CLASS_PATTERN.findAll(cleanedLine).forEach { match ->
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
    }

    return references
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
  }
}
