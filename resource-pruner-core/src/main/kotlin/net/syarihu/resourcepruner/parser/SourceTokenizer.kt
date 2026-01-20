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
   * @param source The source code to process
   * @return The source code with comments and strings replaced by spaces
   */
  fun removeCommentsAndStrings(source: String): String {
    val result = StringBuilder(source.length)
    var i = 0

    while (i < source.length) {
      when {
        // Raw string literal (triple-quoted)
        source.startsWith("\"\"\"", i) -> {
          val endIndex = findRawStringEnd(source, i + 3)
          appendSpaces(result, source, i, endIndex)
          i = endIndex
        }

        // Regular string literal
        source[i] == '"' -> {
          val endIndex = findStringEnd(source, i + 1, '"')
          appendSpaces(result, source, i, endIndex)
          i = endIndex
        }

        // Character literal
        source[i] == '\'' -> {
          val endIndex = findStringEnd(source, i + 1, '\'')
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
   * Finds the end of a raw string literal (triple-quoted).
   */
  private fun findRawStringEnd(
    source: String,
    startIndex: Int,
  ): Int {
    var i = startIndex
    while (i < source.length) {
      if (source.startsWith("\"\"\"", i)) {
        return i + 3
      }
      i++
    }
    return source.length
  }

  /**
   * Finds the end of a string or character literal.
   */
  private fun findStringEnd(
    source: String,
    startIndex: Int,
    delimiter: Char,
  ): Int {
    var i = startIndex
    while (i < source.length) {
      when {
        source[i] == '\\' && i + 1 < source.length -> {
          // Skip escaped character
          i += 2
        }
        source[i] == delimiter -> {
          return i + 1
        }
        source[i] == '\n' -> {
          // String literals can't span lines (except raw strings)
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
}
