package net.syarihu.resourcepruner.detector

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import net.syarihu.resourcepruner.model.ReferencePattern
import net.syarihu.resourcepruner.model.ResourceType
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class ViewBindingUsageDetectorTest : DescribeSpec({
  describe("ViewBindingUsageDetector") {
    val detector = ViewBindingUsageDetector()

    describe("detect") {
      it("should detect ViewBinding.inflate usage") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val kotlinFile = tempDir.resolve("MainActivity.kt")
          kotlinFile.writeText(
            """
            package com.example.app

            import android.os.Bundle
            import com.example.app.databinding.ActivityMainBinding

            class MainActivity : AppCompatActivity() {
                private lateinit var binding: ActivityMainBinding

                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    binding = ActivityMainBinding.inflate(layoutInflater)
                    setContentView(binding.root)
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 3
          references.all { it.resourceName == "activity_main" } shouldBe true
          references.all { it.resourceType == ResourceType.File.Layout } shouldBe true
          references.all { it.location.pattern == ReferencePattern.VIEW_BINDING } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect ViewBinding.bind usage") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val kotlinFile = tempDir.resolve("CustomView.kt")
          kotlinFile.writeText(
            """
            package com.example.app

            class CustomView(context: Context) {
                private val binding = ItemListBinding.bind(this)
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "item_list"
          references.first().resourceType shouldBe ResourceType.File.Layout
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect ViewBinding type declaration") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val kotlinFile = tempDir.resolve("Fragment.kt")
          kotlinFile.writeText(
            """
            package com.example.app

            class HomeFragment : Fragment() {
                private var _binding: FragmentHomeBinding? = null
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "fragment_home"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect ViewBinding in Java files") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val javaFile = tempDir.resolve("MainActivity.java")
          javaFile.writeText(
            """
            package com.example.app;

            public class MainActivity extends AppCompatActivity {
                private ActivityMainBinding binding;

                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    binding = ActivityMainBinding.inflate(getLayoutInflater());
                }
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 2
          references.all { it.resourceName == "activity_main" } shouldBe true
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should not detect non-binding classes ending with Binding") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val kotlinFile = tempDir.resolve("Test.kt")
          kotlinFile.writeText(
            """
            package com.example.app

            // This is just a class name ending with Binding
            class Binding {
                fun doSomething() {}
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          // "Binding" alone should not match because the prefix would be empty
          references.shouldBeEmpty()
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should detect multiple different bindings") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val kotlinFile = tempDir.resolve("App.kt")
          kotlinFile.writeText(
            """
            package com.example.app

            fun setupViews() {
                val main = ActivityMainBinding.inflate(inflater)
                val home = FragmentHomeBinding.inflate(inflater)
                val item = ItemListBinding.inflate(inflater)
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 3
          references.map { it.resourceName }.toSet() shouldBe setOf(
            "activity_main",
            "fragment_home",
            "item_list",
          )
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }

      it("should handle nested directories") {
        val tempDir = Files.createTempDirectory("source")
        try {
          val nestedDir = tempDir.resolve("com/example/app")
          nestedDir.createDirectories()

          val kotlinFile = nestedDir.resolve("MainActivity.kt")
          kotlinFile.writeText(
            """
            class MainActivity {
                val binding = ActivityMainBinding.inflate(inflater)
            }
            """.trimIndent(),
          )

          val references = detector.detect(listOf(tempDir), emptyList())

          references shouldHaveSize 1
          references.first().resourceName shouldBe "activity_main"
        } finally {
          tempDir.toFile().deleteRecursively()
        }
      }
    }

    describe("bindingClassNameToLayoutName") {
      it("should convert ActivityMainBinding to activity_main") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("ActivityMainBinding") shouldBe "activity_main"
      }

      it("should convert FragmentHomeBinding to fragment_home") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("FragmentHomeBinding") shouldBe "fragment_home"
      }

      it("should convert ItemListBinding to item_list") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("ItemListBinding") shouldBe "item_list"
      }

      it("should convert DialogConfirmBinding to dialog_confirm") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("DialogConfirmBinding") shouldBe "dialog_confirm"
      }

      it("should return null for non-Binding classes") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("MyClass") shouldBe null
      }

      it("should return null for just Binding") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("Binding") shouldBe null
      }

      it("should handle single word prefix") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("MainBinding") shouldBe "main"
      }

      it("should handle multiple uppercase letters") {
        ViewBindingUsageDetector.bindingClassNameToLayoutName("ActivityXMLBinding") shouldBe "activity_x_m_l"
      }
    }
  }
})
