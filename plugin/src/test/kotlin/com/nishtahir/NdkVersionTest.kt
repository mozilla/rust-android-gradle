package com.nishtahir

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

data class NdkTestData(
    val ndkVersion: String
) : WithDataTestName {
    override fun dataTestName() =
        "cargoBuild works with Android NDK version $ndkVersion"
}

class NdkVersionTest : FunSpec({
    val androidVersion = TestVersions.latestAndroidVersionForCurrentJDK
    val gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
    val target = "x86_64"
    val location = "android/x86_64/librust.so"

    withData(listOf(
        // Partial list of NDK versions supported by Github Actions, per
        // https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2204-Readme.md
        NdkTestData("26.3.11579264"),
        NdkTestData("27.2.12479018")
    )) { (ndkVersion) ->
        // arrange
        val projectDir = tempdir()

        SimpleAndroidApp(
            projectDir = projectDir,
            androidVersion = androidVersion,
            kotlinVersion = null,
            ndkVersionOverride = VersionNumber.parse(ndkVersion)
        ).writeProject()

        SimpleCargoProject(
            projectDir = projectDir,
            targets = listOf(target)
        ).writeProject()

        // To ease debugging.
        projectDir.walk().onEnter {
            println("before> $it")
            if (it.path.endsWith(".gradle")) {
                println(it.readText())
            }
            true
        }

        // act
        val buildResult = RunGradleTask(
            gradleVersion = gradleVersion,
            projectDir = projectDir,
            taskName = "cargoBuild",
            arguments = listOf("--info", "--stacktrace")
        ).build()

        // To ease debugging.
        projectDir.walk().onEnter {
            println("after> $it")
            true
        }

        // assert
        buildResult.task(":app:cargoBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        buildResult.task(":library:cargoBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        File(projectDir, "app/build/rustJniLibs/${location}") should { it.exists() }
        File(projectDir, "library/build/rustJniLibs/${location}") should { it.exists() }
    }
})