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
 * Detects resource usage through ViewBinding in Kotlin and Java source files.
 *
 * ViewBinding generates classes like:
 * - activity_main.xml → ActivityMainBinding
 * - fragment_home.xml → FragmentHomeBinding
 * - item_list.xml → ItemListBinding
 *
 * This detector finds usages like:
 * - ActivityMainBinding.inflate(layoutInflater)
 * - ActivityMainBinding.bind(view)
 * - val binding: ActivityMainBinding
 * - var binding = ActivityMainBinding.inflate(...)
 */
class ViewBindingUsageDetector : UsageDetector {
  private val tokenizer = SourceTokenizer()

  override fun detect(
    sourceRoots: List<Path>,
    resDirectories: List<Path>,
  ): Set<ResourceReference> {
    return sourceRoots
      .filter { it.exists() && it.isDirectory() }
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

      BINDING_CLASS_PATTERN.findAll(cleanedLine).forEach { match ->
        val bindingClassName = match.groupValues[1]
        val layoutName = bindingClassNameToLayoutName(bindingClassName)

        if (layoutName != null) {
          val column = match.range.first + 1
          references.add(
            ResourceReference(
              resourceName = layoutName,
              resourceType = ResourceType.File.Layout,
              location = ReferenceLocation(
                filePath = file,
                line = lineNumber,
                column = column,
                pattern = ReferencePattern.VIEW_BINDING,
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
     * Pattern to match ViewBinding class references.
     *
     * Matches patterns like:
     * - ActivityMainBinding.inflate
     * - ActivityMainBinding.bind
     * - : ActivityMainBinding (type declaration)
     * - ActivityMainBinding? (nullable type)
     *
     * The pattern captures the binding class name with word boundary.
     */
    private val BINDING_CLASS_PATTERN = Regex(
      """\b([A-Z]\w*Binding)\b""",
    )

    /**
     * Converts a ViewBinding class name to a layout resource name.
     *
     * Examples:
     * - ActivityMainBinding → activity_main
     * - FragmentHomeBinding → fragment_home
     * - ItemListBinding → item_list
     *
     * @return The layout name, or null if the class name doesn't match the expected pattern
     */
    fun bindingClassNameToLayoutName(bindingClassName: String): String? {
      if (!bindingClassName.endsWith("Binding")) {
        return null
      }

      // Remove "Binding" suffix
      val withoutSuffix = bindingClassName.removeSuffix("Binding")

      if (withoutSuffix.isEmpty()) {
        return null
      }

      // Convert PascalCase to snake_case
      return pascalCaseToSnakeCase(withoutSuffix)
    }

    /**
     * Converts PascalCase to snake_case.
     *
     * Examples:
     * - ActivityMain → activity_main
     * - FragmentHome → fragment_home
     * - ItemList → item_list
     */
    private fun pascalCaseToSnakeCase(pascalCase: String): String {
      return buildString {
        pascalCase.forEachIndexed { index, char ->
          if (char.isUpperCase()) {
            if (index > 0) {
              append('_')
            }
            append(char.lowercaseChar())
          } else {
            append(char)
          }
        }
      }
    }
  }
}
