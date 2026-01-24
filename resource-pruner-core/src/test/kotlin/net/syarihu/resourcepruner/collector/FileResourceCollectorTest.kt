package net.syarihu.resourcepruner.collector

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class FileResourceCollectorTest : DescribeSpec({
  describe("FileResourceCollector") {
    val collector = FileResourceCollector()

    describe("collect") {
      it("should collect drawable resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          drawableDir.resolve("ic_launcher.png").writeText("")
          drawableDir.resolve("bg_button.xml").writeText("<shape/>")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("ic_launcher", "bg_button")
          resources.all { it.type == ResourceType.File.Drawable } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect resources with qualifiers") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableHdpiDir = tempDir.resolve("drawable-hdpi").createDirectories()
          drawableHdpiDir.resolve("ic_launcher.png").writeText("")

          val drawableNightDir = tempDir.resolve("drawable-night-hdpi").createDirectories()
          drawableNightDir.resolve("ic_launcher.png").writeText("")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2

          val hdpiResource = resources.find { it.qualifiers == setOf("hdpi") }
          hdpiResource?.name shouldBe "ic_launcher"

          val nightHdpiResource = resources.find { it.qualifiers == setOf("night", "hdpi") }
          nightHdpiResource?.name shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect layout resources") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText("<LinearLayout/>")
          layoutDir.resolve("fragment_home.xml").writeText("<FrameLayout/>")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.all { it.type == ResourceType.File.Layout } shouldBe true
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("activity_main", "fragment_home")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should ignore hidden files") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          drawableDir.resolve("ic_launcher.png").writeText("")
          drawableDir.resolve(".hidden_file.png").writeText("")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 1
          resources.first().name shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should return empty list for non-existent directory") {
        val tempDir = Files.createTempDirectory("res")
        val nonExistent = tempDir.resolve("non_existent")

        val resources = collector.collect(listOf(nonExistent))

        resources.shouldBeEmpty()

        tempDir.toFile().deleteRecursively()
      }

      it("should collect from multiple res directories") {
        val tempDir1 = Files.createTempDirectory("res1")
        val tempDir2 = Files.createTempDirectory("res2")
        try {
          tempDir1.resolve("drawable").createDirectories().resolve("icon1.png").writeText("")
          tempDir2.resolve("drawable").createDirectories().resolve("icon2.png").writeText("")

          val resources = collector.collect(listOf(tempDir1, tempDir2))

          resources shouldHaveSize 2
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf("icon1", "icon2")
        } finally {
          tempDir1.toFile().deleteRecursively()
          tempDir2.toFile().deleteRecursively()
        }
      }

      it("should extract correct resource name from 9-patch images") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          // 9-patch image: the .9 should be removed from the resource name
          drawableDir.resolve("progress_bg.9.png").writeText("")
          drawableDir.resolve("button_normal.9.png").writeText("")
          // Regular image: no .9 suffix
          drawableDir.resolve("ic_launcher.png").writeText("")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 3
          // Resource names should NOT include the .9 suffix
          // e.g., "progress_bg.9.png" -> "progress_bg" (NOT "progress_bg.9")
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "progress_bg",
            "button_normal",
            "ic_launcher",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect raw resources with any file extension") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val rawDir = tempDir.resolve("raw").createDirectories()
          // Raw resources can contain any file type
          rawDir.resolve("sample_audio.mp3").writeText("")
          rawDir.resolve("sample_config.json").writeText("{}")
          rawDir.resolve("sample_data.json").writeText("{}")
          rawDir.resolve("sample_video.mp4").writeText("")
          rawDir.resolve("sample_text.txt").writeText("")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 5
          resources.all { it.type == ResourceType.File.Raw } shouldBe true
          resources.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "sample_audio",
            "sample_config",
            "sample_data",
            "sample_video",
            "sample_text",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should collect raw resources with qualifiers") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val rawDir = tempDir.resolve("raw").createDirectories()
          rawDir.resolve("sample_audio.mp3").writeText("")

          val rawJaDir = tempDir.resolve("raw-ja").createDirectories()
          rawJaDir.resolve("sample_audio.mp3").writeText("")

          val resources = collector.collect(listOf(tempDir))

          resources shouldHaveSize 2
          resources.all { it.type == ResourceType.File.Raw } shouldBe true

          val defaultResource = resources.find { it.qualifiers.isEmpty() }
          defaultResource?.name shouldBe "sample_audio"

          val jaResource = resources.find { it.qualifiers == setOf("ja") }
          jaResource?.name shouldBe "sample_audio"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }
  }
})
