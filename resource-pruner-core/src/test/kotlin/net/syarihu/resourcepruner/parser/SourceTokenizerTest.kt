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

      it("should preserve resource references inside string templates") {
        val source = """
          val text = "${"$"}{stringResource(R.string.hello_world)}"
          val combined = "${"$"}{getString(R.string.first)} ${"$"}{getString(R.string.second)}"
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.string.hello_world"
        result shouldContain "R.string.first"
        result shouldContain "R.string.second"
      }

      it("should preserve resource references in multi-line string templates") {
        val source = """
          ComposableFun(
            text = "${"$"}{stringResource(id = R.string.xxxx_text)} ${"$"}{stringResource(id = R.string.hoge_fuga)}",
          )
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.string.xxxx_text"
        result shouldContain "R.string.hoge_fuga"
      }

      it("should preserve resource references in raw string templates") {
        val source = """
          val text = ${"\"\"\""}
            ${"$"}{stringResource(R.string.in_raw_string)}
          ${"\"\"\""}
          val y = R.string.after_raw
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.string.in_raw_string"
        result shouldContain "R.string.after_raw"
      }

      it("should handle nested template expressions") {
        val source = """
          val text = "${"$"}{if (condition) getString(R.string.yes) else getString(R.string.no)}"
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldContain "R.string.yes"
        result shouldContain "R.string.no"
      }

      it("should still hide string content outside of templates") {
        val source = """
          val text = "Hello R.string.should_be_hidden ${"$"}{R.string.should_be_visible}"
        """.trimIndent()

        val result = tokenizer.removeCommentsAndStrings(source)

        result shouldNotContain "R.string.should_be_hidden"
        result shouldContain "R.string.should_be_visible"
      }
    }
  }
})
