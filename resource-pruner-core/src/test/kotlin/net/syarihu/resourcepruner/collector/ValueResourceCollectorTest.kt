package net.syarihu.resourcepruner.collector

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.syarihu.resourcepruner.model.ResourceLocation
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ValueResourceCollectorTest : DescribeSpec({
  describe("ValueResourceCollector") {
    val collector = ValueResourceCollector()

    describe("collect") {
      it("should collect string resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("strings.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">My App</string>
                <string name="hello_world">Hello World</string>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("app_name", "hello_world")
          resources.all { it.type == ResourceType.Value.StringRes } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect color resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("colors.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="colorPrimary">#6200EE</color>
                <color name="colorAccent">#03DAC5</color>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("colorPrimary", "colorAccent")
          resources.all { it.type == ResourceType.Value.Color } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect dimen resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("dimens.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <dimen name="padding_small">8dp</dimen>
                <dimen name="padding_large">16dp</dimen>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("padding_small", "padding_large")
          resources.all { it.type == ResourceType.Value.Dimen } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect multi-line string resources with correct line numbers") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("strings.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="long_text">
                    This is a very long text
                    that spans multiple lines
                </string>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 1
          val resource = resources.first()
          resource.name shouldBe "long_text"

          val location = resource.location as ResourceLocation.ValueLocation
          location.startLine shouldBe 3
          location.endLine shouldBe 6
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect resources with qualifiers") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("strings.xml").writeText(
            """
            <resources>
                <string name="app_name">My App</string>
            </resources>
            """.trimIndent(),
          )

          val valuesJaDir = tempDir.resolve("values-ja").createDirectories()
          valuesJaDir.resolve("strings.xml").writeText(
            """
            <resources>
                <string name="app_name">マイアプリ</string>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2

          val defaultResource = resources.find { it.qualifiers.isEmpty() }
          defaultResource?.name shouldBe "app_name"

          val jaResource = resources.find { it.qualifiers == setOf("ja") }
          jaResource?.name shouldBe "app_name"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect style resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme" parent="Theme.AppCompat">
                    <item name="colorPrimary">@color/colorPrimary</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 1
          resources.first().name shouldBe "AppTheme"
          resources.first().type shouldBe ResourceType.Value.Style
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect array resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("arrays.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string-array name="planets">
                    <item>Mercury</item>
                    <item>Venus</item>
                </string-array>
                <integer-array name="numbers">
                    <item>1</item>
                    <item>2</item>
                </integer-array>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("planets", "numbers")
          resources.all { it.type == ResourceType.Value.Array } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect item resources with type attribute") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("drawables.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <item name="sample_icon" type="drawable">@drawable/sample_icon</item>
                <item name="sample_background" type="drawable">@drawable/sample_background</item>
                <item name="sample_launcher_alias" type="mipmap">@mipmap/ic_launcher</item>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 3

          val drawableResources = resources.filter { it.type == ResourceType.File.Drawable }
          drawableResources shouldHaveSize 2
          drawableResources.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "sample_icon",
            "sample_background",
          )

          val mipmapResources = resources.filter { it.type == ResourceType.File.Mipmap }
          mipmapResources shouldHaveSize 1
          mipmapResources.first().name shouldBe "sample_launcher_alias"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect item resources with raw type") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("raws.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <item name="sample_config_alias" type="raw">@raw/sample_config</item>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 1
          resources.first().name shouldBe "sample_config_alias"
          resources.first().type shouldBe ResourceType.File.Raw
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should ignore item elements without type attribute (style items)") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme" parent="Theme.AppCompat">
                    <item name="colorPrimary">@color/colorPrimary</item>
                    <item name="colorAccent">@color/colorAccent</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          // Only the style should be collected, not the item elements inside it
          resources shouldHaveSize 1
          resources.first().name shouldBe "AppTheme"
          resources.first().type shouldBe ResourceType.Value.Style
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect self-closing item resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("ids.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <item name="sample_color" type="color">#FF0000</item>
                <item name="sample_dimen" type="dimen">16dp</item>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("sample_color", "sample_dimen")

          val colorResource = resources.find { it.name == "sample_color" }
          colorResource?.type shouldBe ResourceType.Value.Color

          val dimenResource = resources.find { it.name == "sample_dimen" }
          dimenResource?.type shouldBe ResourceType.Value.Dimen
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect drawable tag resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("drawables.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <drawable name="sample_icon_alias">@drawable/sample_icon</drawable>
                <drawable name="sample_bg_alias">@drawable/sample_bg</drawable>
            </resources>
            """.trimIndent(),
          )

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "sample_icon_alias",
            "sample_bg_alias",
          )
          resources.all { it.type == ResourceType.File.Drawable } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }
  }
})
