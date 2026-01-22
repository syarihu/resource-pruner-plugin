package net.syarihu.resourcepruner.detector

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.syarihu.resourcepruner.model.ReferencePattern
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class KotlinUsageDetectorTest : DescribeSpec({
  describe("KotlinUsageDetector") {
    val detector = KotlinUsageDetector()

    describe("detect") {
      it("should detect R.drawable references") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                fun loadIcon() {
                    val icon = R.drawable.ic_launcher
                    val bg = R.drawable.bg_button
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("ic_launcher", "bg_button")
          references.all { it.resourceType == ResourceType.File.Drawable } shouldBe true
          references.all { it.location.pattern == ReferencePattern.KOTLIN_R_CLASS } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect R.string references") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                fun getText() {
                    getString(R.string.app_name)
                    getString(R.string.hello_world)
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("app_name", "hello_world")
          references.all { it.resourceType == ResourceType.Value.StringRes } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect references in Java files") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val javaFile = tempDir.resolve("Test.java")
          javaFile.writeText(
            """
            package com.example;

            public class Test {
                public void loadIcon() {
                    int icon = R.drawable.ic_launcher;
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "ic_launcher"
          references.first().location.pattern shouldBe ReferencePattern.JAVA_R_CLASS
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should ignore references in comments") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                // R.drawable.commented_out should be ignored
                /* R.string.block_comment also ignored */
                fun loadIcon() {
                    val icon = R.drawable.ic_launcher
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should ignore references in string literals") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                val text = "R.drawable.in_string should be ignored"
                fun loadIcon() {
                    val icon = R.drawable.ic_launcher
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect multiple resource types") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                fun setup() {
                    setContentView(R.layout.activity_main)
                    val icon = R.drawable.ic_launcher
                    val text = R.string.app_name
                    val color = R.color.colorPrimary
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 4
          references.map { it.resourceType } shouldContainExactlyInAnyOrder listOf(
            ResourceType.File.Layout,
            ResourceType.File.Drawable,
            ResourceType.Value.StringRes,
            ResourceType.Value.Color,
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should scan nested directories") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val nestedDir = tempDir.resolve("com/example").createDirectories()
          nestedDir.resolve("Test.kt").writeText(
            """
            val icon = R.drawable.nested_icon
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "nested_icon"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect references using import aliases") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            import net.syarihu.android.easypageshare.resources.R as R_resource

            class Test {
                fun getText() {
                    getString(R_resource.string.app_name)
                    getString(R_resource.string.hello_world)
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("app_name", "hello_world")
          references.all { it.resourceType == ResourceType.Value.StringRes } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect both R and aliased references in same file") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            import net.syarihu.other.R as OtherR

            class Test {
                fun setup() {
                    val icon = R.drawable.ic_launcher
                    val otherIcon = OtherR.drawable.ic_other
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("ic_launcher", "ic_other")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect multiple import aliases") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            import net.syarihu.lib1.R as Lib1R
            import net.syarihu.lib2.R as Lib2R

            class Test {
                fun setup() {
                    val icon1 = Lib1R.drawable.ic_lib1
                    val icon2 = Lib2R.drawable.ic_lib2
                    val icon3 = R.drawable.ic_main
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 3
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("ic_lib1", "ic_lib2", "ic_main")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect attr references from R.styleable") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val javaFile = tempDir.resolve("CustomView.java")
          javaFile.writeText(
            """
            package com.example;

            public class CustomView {
                public void init() {
                    int tabBg = a.getResourceId(R.styleable.CustomView_customBackground, 0);
                    int offset = a.getDimensionPixelSize(R.styleable.CustomView_customScrollOffset, 0);
                    int color = a.getColor(R.styleable.CustomView_customIndicatorColor, 0);
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          // Should extract attr names from R.styleable.StyleableName_attrName
          val attrRefs = references.filter { it.resourceType == ResourceType.Value.Attr }
          attrRefs shouldHaveSize 3
          attrRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "customBackground",
            "customScrollOffset",
            "customIndicatorColor",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect resource references with Japanese names") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example

            class Test {
                fun getArray() {
                    val array = resources.getStringArray(R.array.ほげほげ)
                    val string = getString(R.string.日本語リソース名)
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("ほげほげ", "日本語リソース名")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect resource references inside string templates") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Composable.kt")
          kotlinFile.writeText(
            """
            package com.example

            @Composable
            fun MyComposable() {
                Text(
                    text = "${"$"}{stringResource(id = R.string.xxxx_text)} ${"$"}{stringResource(id = R.string.hoge_fuga_piyo)}",
                )
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("xxxx_text", "hoge_fuga_piyo")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect resource references in multi-line string templates") {
        val tempDir = Files.createTempDirectory("src")
        try {
          val kotlinFile = tempDir.resolve("Composable.kt")
          kotlinFile.writeText(
            """
            package com.example

            @Composable
            fun MyComposable() {
                ComposableFun(
                    text =
                        "${"$"}{stringResource(id = R.string.first_resource)} ${"$"}{stringResource(id = R.string.second_resource)}",
                )
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("first_resource", "second_resource")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }
  }
})
