package net.syarihu.resourcepruner.parser

/**
 * Tokenizer for Kotlin/Java source code.
 *
 * This class is used to identify and remove comments and string literals
 * from source code before searching for resource references.
 */
class SourceTokenizer {
  /**
   * Removes comments and string literals from source code.
   *
   * This method replaces:
   * - Single-line comments (// ...)
   * - Multi-line comments (/* ... */)
   * - KDoc comments (/** ... */)
   * - String literals ("..." and """...""")
   * - Character literals ('...')
   *
   * with whitespace to preserve line numbers and positions.
   *
   * For Kotlin string templates, expressions inside ${...} are preserved
   * since they may contain resource references like R.string.xxx.
   *
   * @param source The source code to process
   * @return The source code with comments and strings replaced by spaces
   */
  fun removeCommentsAndStrings(source: String): String {
    val result = StringBuilder(source.length)
    var i = 0

    while (i < source.length) {
      when {
        // Raw string literal (triple-quoted) - may contain templates
        source.startsWith("\"\"\"", i) -> {
          i = processRawStringLiteral(source, i, result)
        }

        // Regular string literal - may contain templates
        source[i] == '"' -> {
          i = processStringLiteral(source, i, result)
        }

        // Character literal
        source[i] == '\'' -> {
          val endIndex = findCharLiteralEnd(source, i + 1)
          appendSpaces(result, source, i, endIndex)
          i = endIndex
        }

        // Single-line comment
        source.startsWith("//", i) -> {
          val endIndex = findLineEnd(source, i)
          appendSpaces(result, source, i, endIndex)
          i = endIndex
        }

        // Multi-line or KDoc comment
        source.startsWith("/*", i) -> {
          val endIndex = findMultiLineCommentEnd(source, i + 2)
          appendSpaces(result, source, i, endIndex)
          i = endIndex
        }

        else -> {
          result.append(source[i])
          i++
        }
      }
    }

    return result.toString()
  }

  /**
   * Processes a regular string literal, preserving template expressions.
   *
   * @return The index after the string literal ends
   */
  private fun processStringLiteral(
    source: String,
    startIndex: Int,
    result: StringBuilder,
  ): Int {
    // Add space for opening quote
    result.append(' ')
    var i = startIndex + 1

    while (i < source.length) {
      when {
        // Escaped character
        source[i] == '\\' && i + 1 < source.length -> {
          result.append(' ')
          result.append(' ')
          i += 2
        }
        // Template expression - preserve the expression content
        source.startsWith("\${", i) -> {
          result.append(' ') // for $
          result.append(' ') // for {
          i += 2
          i = processTemplateExpression(source, i, result)
        }
        // Simple template variable ($identifier)
        source[i] == '$' && i + 1 < source.length && isIdentifierStart(source[i + 1]) -> {
          result.append(' ') // for $
          i++
          // Preserve the identifier
          while (i < source.length && isIdentifierPart(source[i])) {
            result.append(source[i])
            i++
          }
        }
        // End of string
        source[i] == '"' -> {
          result.append(' ')
          return i + 1
        }
        // Newline - string literals can't span lines (except raw strings)
        source[i] == '\n' -> {
          result.append('\n')
          return i + 1
        }
        // Regular character - replace with space
        else -> {
          result.append(' ')
          i++
        }
      }
    }
    return source.length
  }

  /**
   * Processes a raw string literal (triple-quoted), preserving template expressions.
   *
   * @return The index after the string literal ends
   */
  private fun processRawStringLiteral(
    source: String,
    startIndex: Int,
    result: StringBuilder,
  ): Int {
    // Add spaces for opening quotes
    result.append("   ")
    var i = startIndex + 3

    while (i < source.length) {
      when {
        // End of raw string
        source.startsWith("\"\"\"", i) -> {
          result.append("   ")
          return i + 3
        }
        // Template expression - preserve the expression content
        source.startsWith("\${", i) -> {
          result.append(' ') // for $
          result.append(' ') // for {
          i += 2
          i = processTemplateExpression(source, i, result)
        }
        // Simple template variable ($identifier)
        source[i] == '$' && i + 1 < source.length && isIdentifierStart(source[i + 1]) -> {
          result.append(' ') // for $
          i++
          // Preserve the identifier
          while (i < source.length && isIdentifierPart(source[i])) {
            result.append(source[i])
            i++
          }
        }
        // Newline - preserve for line tracking
        source[i] == '\n' -> {
          result.append('\n')
          i++
        }
        // Regular character - replace with space
        else -> {
          result.append(' ')
          i++
        }
      }
    }
    return source.length
  }

  /**
   * Processes a template expression ${...}, preserving its content.
   * Handles nested braces and nested strings.
   *
   * @return The index after the closing brace
   */
  private fun processTemplateExpression(
    source: String,
    startIndex: Int,
    result: StringBuilder,
  ): Int {
    var i = startIndex
    var braceDepth = 1

    while (i < source.length && braceDepth > 0) {
      when {
        source[i] == '{' -> {
          result.append(source[i])
          braceDepth++
          i++
        }
        source[i] == '}' -> {
          braceDepth--
          if (braceDepth > 0) {
            result.append(source[i])
          } else {
            result.append(' ') // Replace closing } with space
          }
          i++
        }
        // Nested string inside template - recursively process
        source[i] == '"' && !source.startsWith("\"\"\"", i) -> {
          i = processStringLiteral(source, i, result)
        }
        // Nested raw string inside template
        source.startsWith("\"\"\"", i) -> {
          i = processRawStringLiteral(source, i, result)
        }
        source[i] == '\n' -> {
          result.append('\n')
          i++
        }
        else -> {
          result.append(source[i])
          i++
        }
      }
    }
    return i
  }

  /**
   * Finds the end of a character literal.
   */
  private fun findCharLiteralEnd(
    source: String,
    startIndex: Int,
  ): Int {
    var i = startIndex
    while (i < source.length) {
      when {
        source[i] == '\\' && i + 1 < source.length -> {
          // Skip escaped character
          i += 2
        }
        source[i] == '\'' -> {
          return i + 1
        }
        source[i] == '\n' -> {
          return i
        }
        else -> {
          i++
        }
      }
    }
    return source.length
  }

  /**
   * Finds the end of a line (newline character).
   */
  private fun findLineEnd(
    source: String,
    startIndex: Int,
  ): Int {
    var i = startIndex
    while (i < source.length && source[i] != '\n') {
      i++
    }
    return i
  }

  /**
   * Finds the end of a multi-line comment.
   */
  private fun findMultiLineCommentEnd(
    source: String,
    startIndex: Int,
  ): Int {
    var i = startIndex
    while (i < source.length) {
      if (source.startsWith("*/", i)) {
        return i + 2
      }
      i++
    }
    return source.length
  }

  /**
   * Appends spaces to the result, preserving newlines for line number tracking.
   */
  private fun appendSpaces(
    result: StringBuilder,
    source: String,
    startIndex: Int,
    endIndex: Int,
  ) {
    for (i in startIndex until endIndex) {
      if (source[i] == '\n') {
        result.append('\n')
      } else {
        result.append(' ')
      }
    }
  }

  /**
   * Checks if a character can start an identifier.
   */
  private fun isIdentifierStart(c: Char): Boolean {
    return c.isLetter() || c == '_'
  }

  /**
   * Checks if a character can be part of an identifier.
   */
  private fun isIdentifierPart(c: Char): Boolean {
    return c.isLetterOrDigit() || c == '_'
  }
}
