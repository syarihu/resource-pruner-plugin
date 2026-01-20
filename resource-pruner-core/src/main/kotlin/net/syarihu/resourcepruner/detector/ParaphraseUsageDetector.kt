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
 * Detects resource usage through Paraphrase library in Kotlin and Java source files.
 *
 * Paraphrase generates a FormattedResources object with methods for each ICU-formatted string.
 * This detector finds usages like:
 * - FormattedResources.greeting_format(name = "World")
 * - FormattedResources.count_format(count = 42)
 *
 * And maps them to the corresponding string resources:
 * - greeting_format → R.string.greeting_format
 * - count_format → R.string.count_format
 *
 * IMPORTANT: This detector should only scan non-generated source directories.
 * The Paraphrase-generated FormattedResources.kt contains R.string.xxx references,
 * but those should NOT be considered as "used" - only actual calls to
 * FormattedResources.xxx() in user code indicate real usage.
 */
class ParaphraseUsageDetector : UsageDetector {
  private val tokenizer = SourceTokenizer()

  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    return sourceRoots
      .filter { it.exists() && it.isDirectory() }
      .filter { !isParaphraseGeneratedDirectory(it) }
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
    val cleanedLines = cleanedContent.lines()

    val references = mutableListOf<ResourceReference>()

    cleanedLines.forEachIndexed { lineIndex, cleanedLine ->
      val lineNumber = lineIndex + 1

      FORMATTED_RESOURCES_PATTERN.findAll(cleanedLine).forEach { match ->
        val resourceName = match.groupValues[1]
        val column = match.range.first + 1

        references.add(
          ResourceReference(
            resourceName = resourceName,
            resourceType = ResourceType.Value.StringRes,
            location = ReferenceLocation(
              filePath = file,
              line = lineNumber,
              column = column,
              pattern = ReferencePattern.PARAPHRASE,
            ),
          ),
        )
      }
    }

    return references
  }

  companion object {
    private val SUPPORTED_EXTENSIONS = setOf("kt", "java")

    /**
     * Pattern to match FormattedResources method calls.
     *
     * Matches patterns like:
     * - FormattedResources.greeting_format(
     * - FormattedResources.count_format(
     *
     * The pattern captures the method name (which corresponds to the string resource name).
     */
    private val FORMATTED_RESOURCES_PATTERN = Regex(
      """FormattedResources\.(\w+)\s*\(""",
    )

    /**
     * Checks if a directory is a Paraphrase-generated source directory.
     *
     * Paraphrase generates code in directories like:
     * - build/generated/source/paraphrase/debug/
     * - build/generated/source/paraphrase/release/
     */
    fun isParaphraseGeneratedDirectory(path: Path): Boolean {
      val pathString = path.toString()
      return pathString.contains("/generated/") &&
        pathString.contains("/paraphrase/")
    }
  }
}
