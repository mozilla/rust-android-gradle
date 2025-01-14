package com.nishtahir

import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.WithDataTestName
import io.kotest.datatest.withData
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import java.io.File

data class BuildTestData(
    val android: VersionNumber,
    val gradle: GradleVersion
) : WithDataTestName {
    override fun dataTestName() =
        "cargoBuild is invoked with $gradle and Android plugin $android"
}

@EnabledIf(MultiVersionCondition::class)
class CargoBuildTest : FunSpec({
    val kotlinVersion = TestVersions.latestKotlinVersion

    withData(
        TestVersions.allCandidateTestVersions.flatMap { entry ->
            entry.value.map { BuildTestData(entry.key, it) }
        }
    ) { (androidVersion, gradleVersion) ->
        // arrange
        val projectDir = tempDirectory()

        SimpleAndroidApp(
            projectDir = projectDir,
            androidVersion = androidVersion,
            kotlinVersion = kotlinVersion
        ).writeProject()

        SimpleCargoProject(
            projectDir = projectDir,
            targets = listOf("x86_64")
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
        File(projectDir, "app/build/rustJniLibs/android/x86_64/librust.so") should { it.exists() }
        File(projectDir, "library/build/rustJniLibs/android/x86_64/librust.so") should { it.exists() }
    }
})