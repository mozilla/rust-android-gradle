package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File

val systemDefaultAndroidSdkHome = run {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    if (os.contains("win")) {
        val localappdata = System.getenv("LOCALAPPDATA")
        File("$localappdata\\Android\\Sdk")
    } else if (os.contains("osx")) {
        File("$home/Library/Android/sdk")
    } else {
        File("$home/Android/sdk")
    }
}

class RunGradleTask(
    val gradleVersion: GradleVersion,
    val projectDir: File,
    val taskName: String,
    val arguments: List<String> = listOf("--info", "--stacktrace"),
) {
    private val gradleVersionString = run {
        val str = gradleVersion.version
        if (str.count { it == '.' } == 2 && str.endsWith(".0")) {
            str.substring(0, str.length - 2)
        } else {
            str
        }
    }

    private val environment = run {
        if (System.getenv("ANDROID_HOME").isNullOrBlank()) {
            val sdk = systemDefaultAndroidSdkHome.absolutePath.replace('\\', '/')
            System.getenv() + mapOf("ANDROID_HOME" to sdk)
        } else {
            System.getenv()
        }
    }

    fun build(): BuildResult = GradleRunner.create()
        .withEnvironment(environment)
        .withGradleVersion(gradleVersionString)
        .forwardOutput()
        .withProjectDir(projectDir)
        .withArguments(listOf(taskName) + arguments)
        .build()
}