package net.syarihu.resourcepruner.pruner

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.syarihu.resourcepruner.model.DetectedResource
import net.syarihu.resourcepruner.model.ReferenceLocation
import net.syarihu.resourcepruner.model.ReferencePattern
import net.syarihu.resourcepruner.model.ResourceLocation
import net.syarihu.resourcepruner.model.ResourceReference
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class DefaultResourcePrunerTest : DescribeSpec({
  describe("DefaultResourcePruner") {
    val pruner = DefaultResourcePruner()

    describe("analyze") {
      it("should mark unused resources for removal") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableFile = tempDir.resolve("drawable/icon.png")
          drawableFile.parent.createDirectories()
          drawableFile.writeText("")

          val resources = listOf(
            DetectedResource(
              name = "icon",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(drawableFile),
            ),
          )

          val references = emptySet<ResourceReference>()
          val excludePatterns = emptyList<Regex>()

          val analysis = pruner.analyze(resources, references, excludePatterns)

          analysis.toBeRemoved shouldHaveSize 1
          analysis.toBeRemoved.first().name shouldBe "icon"
          analysis.toBePreserved.shouldBeEmpty()
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should preserve referenced resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableFile = tempDir.resolve("drawable/icon.png")
          drawableFile.parent.createDirectories()
          drawableFile.writeText("")

          val resources = listOf(
            DetectedResource(
              name = "icon",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(drawableFile),
            ),
          )

          val references = setOf(
            ResourceReference(
              resourceName = "icon",
              resourceType = ResourceType.File.Drawable,
              location = ReferenceLocation(
                filePath = tempDir.resolve("Test.kt"),
                line = 1,
                column = 1,
                pattern = ReferencePattern.KOTLIN_R_CLASS,
              ),
            ),
          )

          val excludePatterns = emptyList<Regex>()

          val analysis = pruner.analyze(resources, references, excludePatterns)

          analysis.toBeRemoved.shouldBeEmpty()
          analysis.toBePreserved shouldHaveSize 1
          analysis.toBePreserved.first().name shouldBe "icon"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should preserve resources matching exclude patterns") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableFile = tempDir.resolve("drawable/ic_launcher.png")
          drawableFile.parent.createDirectories()
          drawableFile.writeText("")

          val resources = listOf(
            DetectedResource(
              name = "ic_launcher",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(drawableFile),
            ),
          )

          val references = emptySet<ResourceReference>()
          val excludePatterns = listOf(Regex("^ic_launcher.*"))

          val analysis = pruner.analyze(resources, references, excludePatterns)

          analysis.toBeRemoved.shouldBeEmpty()
          analysis.toBePreserved shouldHaveSize 1
          analysis.toBePreserved.first().name shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should handle mixed resources correctly") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val usedFile = tempDir.resolve("drawable/used_icon.png")
          val unusedFile = tempDir.resolve("drawable/unused_icon.png")
          val excludedFile = tempDir.resolve("drawable/ic_launcher.png")
          usedFile.parent.createDirectories()
          usedFile.writeText("")
          unusedFile.writeText("")
          excludedFile.writeText("")

          val resources = listOf(
            DetectedResource(
              name = "used_icon",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(usedFile),
            ),
            DetectedResource(
              name = "unused_icon",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(unusedFile),
            ),
            DetectedResource(
              name = "ic_launcher",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(excludedFile),
            ),
          )

          val references = setOf(
            ResourceReference(
              resourceName = "used_icon",
              resourceType = ResourceType.File.Drawable,
              location = ReferenceLocation(
                filePath = tempDir.resolve("Test.kt"),
                line = 1,
                column = 1,
                pattern = ReferencePattern.KOTLIN_R_CLASS,
              ),
            ),
          )

          val excludePatterns = listOf(Regex("^ic_launcher.*"))

          val analysis = pruner.analyze(resources, references, excludePatterns)

          analysis.toBeRemoved shouldHaveSize 1
          analysis.toBeRemoved.first().name shouldBe "unused_icon"
          analysis.toBePreserved shouldHaveSize 2
          analysis.toBePreserved.map { it.name }.toSet() shouldBe setOf("used_icon", "ic_launcher")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }

    describe("execute") {
      it("should delete unused file resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val iconFile = tempDir.resolve("drawable/icon.png")
          iconFile.parent.createDirectories()
          iconFile.writeText("PNG content")

          val resources = listOf(
            DetectedResource(
              name = "icon",
              type = ResourceType.File.Drawable,
              location = ResourceLocation.FileLocation(iconFile),
            ),
          )

          val analysis = PruneAnalysis(
            toBeRemoved = resources,
            toBePreserved = emptyList(),
          )

          val report = pruner.execute(analysis)

          report.prunedCount shouldBe 1
          report.errorCount shouldBe 0
          iconFile.exists() shouldBe false
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should remove value resources from XML") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val stringsFile = tempDir.resolve("values/strings.xml")
          stringsFile.parent.createDirectories()
          stringsFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">My App</string>
                <string name="unused_string">Unused</string>
                <string name="used_string">Used</string>
            </resources>
            """.trimIndent(),
          )

          val resources = listOf(
            DetectedResource(
              name = "unused_string",
              type = ResourceType.Value.StringRes,
              location = ResourceLocation.ValueLocation(
                filePath = stringsFile,
                startLine = 4,
                endLine = 4,
                elementXml = """    <string name="unused_string">Unused</string>""",
              ),
            ),
          )

          val analysis = PruneAnalysis(
            toBeRemoved = resources,
            toBePreserved = emptyList(),
          )

          val report = pruner.execute(analysis)

          report.prunedCount shouldBe 1
          report.errorCount shouldBe 0

          val content = stringsFile.readText()
          content.contains("app_name") shouldBe true
          content.contains("used_string") shouldBe true
          content.contains("unused_string") shouldBe false
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should remove multiple value resources from same file") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val stringsFile = tempDir.resolve("values/strings.xml")
          stringsFile.parent.createDirectories()
          stringsFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">My App</string>
                <string name="unused1">Unused 1</string>
                <string name="unused2">Unused 2</string>
            </resources>
            """.trimIndent(),
          )

          val resources = listOf(
            DetectedResource(
              name = "unused1",
              type = ResourceType.Value.StringRes,
              location = ResourceLocation.ValueLocation(
                filePath = stringsFile,
                startLine = 4,
                endLine = 4,
                elementXml = """    <string name="unused1">Unused 1</string>""",
              ),
            ),
            DetectedResource(
              name = "unused2",
              type = ResourceType.Value.StringRes,
              location = ResourceLocation.ValueLocation(
                filePath = stringsFile,
                startLine = 5,
                endLine = 5,
                elementXml = """    <string name="unused2">Unused 2</string>""",
              ),
            ),
          )

          val analysis = PruneAnalysis(
            toBeRemoved = resources,
            toBePreserved = emptyList(),
          )

          val report = pruner.execute(analysis)

          report.prunedCount shouldBe 2
          report.errorCount shouldBe 0

          val content = stringsFile.readText()
          content.contains("app_name") shouldBe true
          content.contains("unused1") shouldBe false
          content.contains("unused2") shouldBe false
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }
  }
})
