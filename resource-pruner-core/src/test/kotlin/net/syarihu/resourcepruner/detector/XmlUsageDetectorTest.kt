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

class XmlUsageDetectorTest : DescribeSpec({
  describe("XmlUsageDetector") {
    val detector = XmlUsageDetector()

    describe("detect") {
      it("should detect drawable references in layout files") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout>
                <ImageView android:src="@drawable/ic_launcher" />
                <ImageView android:background="@drawable/bg_button" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("ic_launcher", "bg_button")
          references.all { it.resourceType == ResourceType.File.Drawable } shouldBe true
          references.all { it.location.pattern == ReferencePattern.XML_REFERENCE } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect string references") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout>
                <TextView android:text="@string/app_name" />
                <TextView android:hint="@string/hint_text" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("app_name", "hint_text")
          references.all { it.resourceType == ResourceType.Value.StringRes } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect color references") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout android:background="@color/colorPrimary">
                <View android:background="@color/colorAccent" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("colorPrimary", "colorAccent")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect layout references (include)") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout>
                <include layout="@layout/toolbar" />
                <include layout="@layout/footer" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("toolbar", "footer")
          references.all { it.resourceType == ResourceType.File.Layout } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect style references") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout>
                <TextView style="@style/TextAppearance.Title" />
                <Button style="@style/Widget.Button" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          val styleRefs = references.filter { it.resourceType == ResourceType.Value.Style }
          styleRefs shouldHaveSize 2
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should ignore tools: namespace attributes") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout
                xmlns:tools="http://schemas.android.com/tools"
                tools:background="@drawable/preview_bg">
                <ImageView android:src="@drawable/ic_launcher" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 1
          references.first().resourceName shouldBe "ic_launcher"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect references in drawable XML files") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          drawableDir.resolve("layered.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <layer-list>
                <item android:drawable="@drawable/bg_layer" />
                <item android:drawable="@color/overlay" />
            </layer-list>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("bg_layer", "overlay")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect references in values XML files") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme">
                    <item name="colorPrimary">@color/colorPrimary</item>
                    <item name="android:windowBackground">@drawable/bg_window</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("colorPrimary", "bg_window")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should scan multiple res directories") {
        val tempDir1 = Files.createTempDirectory("res1")
        val tempDir2 = Files.createTempDirectory("res2")
        try {
          val layoutDir1 = tempDir1.resolve("layout").createDirectories()
          layoutDir1.resolve("activity_main.xml").writeText(
            """
            <LinearLayout>
                <ImageView android:src="@drawable/icon1" />
            </LinearLayout>
            """.trimIndent(),
          )

          val layoutDir2 = tempDir2.resolve("layout").createDirectories()
          layoutDir2.resolve("fragment.xml").writeText(
            """
            <LinearLayout>
                <ImageView android:src="@drawable/icon2" />
            </LinearLayout>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir1, tempDir2))

          references shouldHaveSize 2
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("icon1", "icon2")
        } finally {
          tempDir1.toFile().deleteRecursively()
          tempDir2.toFile().deleteRecursively()
        }
      }
    }
  }
})
