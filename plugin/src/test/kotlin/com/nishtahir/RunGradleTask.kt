package com.nishtahir

import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import java.io.File

class RunGradleTask(
    val gradleVersion: GradleVersion,
    val projectDir: File,
    val taskName: String,
    val arguments: List<String> = listOf("--info", "--stacktrace"),
    val debug: Boolean = false
) {
    private val gradleVersionString = run {
        val str = gradleVersion.version
        if (str.count { it == '.' } == 2 && str.endsWith(".0")) {
            str.substring(0, str.length - 2)
        } else {
            str
        }
    }

    fun build() = GradleRunner.create()
        .withGradleVersion(gradleVersionString)
        .forwardOutput()
        .withProjectDir(projectDir)
        .withArguments(taskName + arguments)
        .withDebug(debug)
        .build()
}