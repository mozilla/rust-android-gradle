package com.nishtahir

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import java.io.File

data class TargetTestData(
    val target: String,
    val location: String
) : WithDataTestName {
    override fun dataTestName() =
        "cargoBuild produces $location for target $target"
}

class CargoTargetTest : FunSpec({
    val androidVersion = TestVersions.latestAndroidVersionForCurrentJDK
    val gradleVersion = TestVersions.latestSupportedGradleVersionFor(androidVersion)
    val kotlinVersion = TestVersions.latestKotlinVersion

    withData(listOf(
        // Sadly, cross-compiling to macOS targets fails at this time: see
        // https://github.com/rust-lang/rust/issues/84984.
        // TargetTestTuple("darwin", "desktop/darwin/librust.dylib"),
        // And so does cross-compiling from macOS to Linux targets.
        // TargetTestTuple("linux-x86-64", "destkop/linux-x86-64/librust.so"),
        TargetTestData("arm64", "android/arm64-v8a/librust.so"),
        TargetTestData("x86_64", "android/x86_64/librust.so")
    )) { (target, location) ->
        // arrange
        val projectDir = tempDirectory()

        SimpleAndroidApp(
            projectDir = projectDir,
            androidVersion = androidVersion,
            kotlinVersion = kotlinVersion
        ).writeProject()

        SimpleCargoProject(
            projectDir = projectDir,
            targets = listOf(target),
        ).writeProject()

        // act
        val buildResult = RunGradleTask(
            gradleVersion = gradleVersion,
            projectDir = projectDir,
            taskName = "cargoBuild",
            arguments = listOf("--info", "--stacktrace")
        ).build()

        // To ease debugging.
        projectDir.walk().onEnter {
            println(it)
            true
        }

        // assert
        buildResult.task(":app:cargoBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        buildResult.task(":library:cargoBuild")?.outcome shouldBe TaskOutcome.SUCCESS
        File(projectDir, "app/build/rustJniLibs/${location}") should { it.exists() }
        File(projectDir, "library/build/rustJniLibs/${location}") should { it.exists() }
    }
})

