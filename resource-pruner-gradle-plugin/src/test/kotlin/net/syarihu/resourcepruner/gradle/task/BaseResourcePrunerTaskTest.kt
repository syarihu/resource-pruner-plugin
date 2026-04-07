package net.syarihu.resourcepruner.gradle.task

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Path

class BaseResourcePrunerTaskTest : DescribeSpec({
  describe("BaseResourcePrunerTask") {
    describe("filterSourceResDirectories") {
      val project = ProjectBuilder.builder().build()
      val task = project.tasks.register("testTask", TestResourcePrunerTask::class.java).get()
      val buildDir = project.layout.buildDirectory.get().asFile.toPath()

      it("should exclude resource directories under the build directory") {
        val buildResDir = buildDir.resolve("generated/res/google-services/debug")
        val resDirs = listOf(buildResDir)

        val result = task.testFilterSourceResDirectories(resDirs)

        result.shouldBeEmpty()
      }

      it("should keep resource directories outside the build directory") {
        val srcResDir = project.projectDir.toPath().resolve("src/main/res")
        val resDirs = listOf(srcResDir)

        val result = task.testFilterSourceResDirectories(resDirs)

        result shouldHaveSize 1
        result.first() shouldBe srcResDir
      }

      it("should correctly filter mixed directories") {
        val srcResDir = project.projectDir.toPath().resolve("src/main/res")
        val srcDebugResDir = project.projectDir.toPath().resolve("src/debug/res")
        val buildResDir1 = buildDir.resolve("generated/res/google-services/debug")
        val buildResDir2 = buildDir.resolve("generated/res/firebase/debug")
        val resDirs = listOf(srcResDir, buildResDir1, srcDebugResDir, buildResDir2)

        val result = task.testFilterSourceResDirectories(resDirs)

        result shouldContainExactlyInAnyOrder listOf(srcResDir, srcDebugResDir)
      }

      it("should return empty list when all directories are under build") {
        val buildResDir1 = buildDir.resolve("generated/res/google-services/debug")
        val buildResDir2 = buildDir.resolve("intermediates/res/merged/debug")
        val resDirs = listOf(buildResDir1, buildResDir2)

        val result = task.testFilterSourceResDirectories(resDirs)

        result.shouldBeEmpty()
      }

      it("should return all directories when none are under build") {
        val srcMainRes = project.projectDir.toPath().resolve("src/main/res")
        val srcDebugRes = project.projectDir.toPath().resolve("src/debug/res")
        val resDirs = listOf(srcMainRes, srcDebugRes)

        val result = task.testFilterSourceResDirectories(resDirs)

        result shouldHaveSize 2
        result shouldContainExactlyInAnyOrder listOf(srcMainRes, srcDebugRes)
      }
    }
  }
})

/**
 * Concrete subclass to expose the protected method for testing.
 */
abstract class TestResourcePrunerTask : BaseResourcePrunerTask() {
  fun testFilterSourceResDirectories(resDirs: List<Path>): List<Path> {
    return filterSourceResDirectories(resDirs)
  }
}
