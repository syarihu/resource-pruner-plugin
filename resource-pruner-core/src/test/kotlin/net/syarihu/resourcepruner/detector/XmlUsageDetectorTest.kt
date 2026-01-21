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

      it("should detect drawable references inside nested elements like scale") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          drawableDir.resolve("progress_horizontal.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:id="@android:id/background"
                      android:drawable="@drawable/progress_bg" />
                <item android:id="@android:id/secondaryProgress">
                    <scale android:scaleWidth="100%"
                           android:drawable="@drawable/progress_secondary" />
                </item>
                <item android:id="@android:id/progress">
                    <scale android:scaleWidth="100%"
                           android:drawable="@drawable/progress_primary" />
                </item>
            </layer-list>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          val drawableRefs = references.filter { it.resourceType == ResourceType.File.Drawable }
          drawableRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "progress_bg",
            "progress_secondary",
            "progress_primary",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect drawable references inside style item values") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="CustomProgressBarStyle" parent="android:Widget.Holo.Light.ProgressBar.Horizontal">
                    <item name="android:progressDrawable">@drawable/custom_progress_horizontal</item>
                    <item name="android:indeterminateDrawable">@drawable/custom_progress_indeterminate</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          val drawableRefs = references.filter { it.resourceType == ResourceType.File.Drawable }
          drawableRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "custom_progress_horizontal",
            "custom_progress_indeterminate",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect full reference chain: layout -> style -> drawable -> nested drawable") {
        val tempDir = Files.createTempDirectory("res")
        try {
          // Layout referencing a style
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout>
                <ProgressBar style="@style/CustomProgressBarStyle" />
            </LinearLayout>
            """.trimIndent(),
          )

          // Style referencing a drawable
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="CustomProgressBarStyle">
                    <item name="android:progressDrawable">@drawable/progress_horizontal</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          // Drawable (layer-list) referencing other drawables
          val drawableDir = tempDir.resolve("drawable").createDirectories()
          drawableDir.resolve("progress_horizontal.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:drawable="@drawable/progress_bg" />
                <item>
                    <scale android:drawable="@drawable/progress_primary" />
                </item>
            </layer-list>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          // Should detect all references in the chain
          val styleRefs = references.filter { it.resourceType == ResourceType.Value.Style }
          styleRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("CustomProgressBarStyle")

          val drawableRefs = references.filter { it.resourceType == ResourceType.File.Drawable }
          // progress_horizontal - from style
          // progress_bg - from layer-list
          // progress_primary - from scale inside layer-list
          drawableRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "progress_horizontal",
            "progress_bg",
            "progress_primary",
          )
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

          // colorPrimary appears twice:
          // - as @color/colorPrimary (color reference)
          // - as <item name="colorPrimary"> (attr reference)
          // bg_window appears as @drawable/bg_window (drawable reference)
          // android:windowBackground is skipped (android: prefix)
          references.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("colorPrimary", "colorPrimary", "bg_window")
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

      it("should detect style parent references with dot notation") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme.Base" />
                <style name="AppTheme.Base.ActionBar" parent="AppTheme.Base" />
                <style name="MyStyle.Bold" parent="MyStyle" />
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          // Should detect:
          // - AppTheme.Base from parent="AppTheme.Base"
          // - AppTheme from implicit dot notation in AppTheme.Base
          // - AppTheme.Base from implicit dot notation in AppTheme.Base.ActionBar
          // - AppTheme from implicit dot notation in AppTheme.Base.ActionBar
          // - MyStyle from parent="MyStyle"
          // - MyStyle from implicit dot notation in MyStyle.Bold
          val styleRefs = references.filter { it.resourceType == ResourceType.Value.Style }
          styleRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "AppTheme.Base",
            "AppTheme",
            "AppTheme.Base",
            "AppTheme",
            "MyStyle",
            "MyStyle",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect theme attribute references with ?attr/ prefix") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val layoutDir = tempDir.resolve("layout").createDirectories()
          layoutDir.resolve("activity_main.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout
                android:background="?attr/colorPrimary"
                android:textColor="?attr/colorOnPrimary" />
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          val attrRefs = references.filter { it.resourceType == ResourceType.Value.Attr }
          attrRefs shouldHaveSize 2
          attrRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf("colorPrimary", "colorOnPrimary")
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect library attribute references in style items") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="CustomTabStyle">
                    <item name="customIndicatorColor">@color/primary</item>
                    <item name="customUnderlineColor">@color/secondary</item>
                    <item name="android:textColor">@color/text</item>
                </style>
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          // Should detect attr references from <item name="xxx">
          // android: prefixed items should be skipped
          val attrRefs = references.filter { it.resourceType == ResourceType.Value.Attr }
          attrRefs shouldHaveSize 2
          attrRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "customIndicatorColor",
            "customUnderlineColor",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should not detect framework style parents") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("styles.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="AppTheme" parent="Theme.Material.Light" />
                <style name="ButtonStyle" parent="Widget.AppCompat.Button" />
                <style name="TextStyle" parent="TextAppearance.AppCompat.Body1" />
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          // Framework style parents should NOT be detected as references
          // Theme., Widget., TextAppearance. are known framework prefixes
          val styleRefs = references.filter { it.resourceType == ResourceType.Value.Style }
          styleRefs shouldHaveSize 0
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect attr references inside declare-styleable") {
        val tempDir = Files.createTempDirectory("res")
        try {
          val valuesDir = tempDir.resolve("values").createDirectories()
          valuesDir.resolve("attrs.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <attr name="customBackground" format="reference"/>
                <attr name="customScrollOffset" format="dimension"/>

                <declare-styleable name="CustomView">
                    <attr name="customBackground"/>
                    <attr name="customScrollOffset"/>
                    <attr name="android:textColor"/>
                </declare-styleable>
            </resources>
            """.trimIndent(),
          )

          val references = detector.detect(emptyList(), listOf(tempDir))

          // Should detect attr references from <attr name="xxx"/> (both global and inside declare-styleable)
          // android: prefixed attrs should be skipped
          // Global attrs: customBackground, customScrollOffset (2)
          // Declare-styleable attrs: customBackground, customScrollOffset (2, duplicates)
          val attrRefs = references.filter { it.resourceType == ResourceType.Value.Attr }
          // Global attrs: customBackground, customScrollOffset
          // Declare-styleable attrs: customBackground, customScrollOffset (duplicates)
          attrRefs.map { it.resourceName } shouldContainExactlyInAnyOrder listOf(
            "customBackground",
            "customScrollOffset",
            "customBackground",
            "customScrollOffset",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }
  }
})
