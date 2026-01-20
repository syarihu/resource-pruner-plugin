package net.syarihu.resourcepruner.parser

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class SourceTokenizerTest : DescribeSpec({
  describe("SourceTokenizer") {
    val tokenizer = SourceTokenizer()

    describe("removeCommentsAndStrings") {
      it("should remove single-line comments") {
        val source = """
          val x = R.drawable.icon // This is a comment with R.string.test
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
        result shouldNotContain "R.string.test"
      }

      it("should remove multi-line comments") {
        val source = """
          val x = R.drawable.icon
          /* This is a comment
             with R.string.hidden inside */
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
        result shouldNotContain "R.string.hidden"
      }

      it("should remove KDoc comments") {
        val source = """
          /**
           * This is KDoc with R.drawable.hidden
           */
          val x = R.drawable.icon
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldNotContain "R.drawable.hidden"
      }

      it("should remove string literals") {
        val source = """
          val x = R.drawable.icon
          val text = "This string contains R.string.hidden"
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
        result shouldNotContain "R.string.hidden"
      }

      it("should remove raw string literals") {
        val source = """
          val x = R.drawable.icon
          val text = ${"\"\"\""}
            This raw string contains R.string.hidden
          ${"\"\"\""}
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
        result shouldNotContain "R.string.hidden"
      }

      it("should handle escaped characters in strings") {
        val source = """
          val x = R.drawable.icon
          val text = "Contains \"escaped\" quotes and R.string.hidden"
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
        result shouldNotContain "R.string.hidden"
      }

      it("should preserve line numbers") {
        val source = """
          line1
          line2 // comment
          line3
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)
        val originalLineCount = source.lines().size
        val resultLineCount = result.lines().size

        resultLineCount shouldBe originalLineCount
      }

      it("should handle character literals") {
        val source = """
          val x = R.drawable.icon
          val c = 'x'
          val y = R.string.hello
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.drawable.icon"
        result shouldContain "R.string.hello"
      }
    }
  }
})
