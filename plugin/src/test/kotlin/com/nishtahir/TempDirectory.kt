package com.nishtahir

import io.kotest.core.TestConfiguration
import io.kotest.core.test.TestResult
import java.io.File

fun TestConfiguration.tempDirectory(prefix: String? = null, suffix: String? = null): File {
    val dir = kotlin.io.path.createTempDirectory(prefix ?: javaClass.name).toFile()
    afterTest { (_, res) ->
        if (res is TestResult.Success || res is TestResult.Ignored) {
            dir.deleteRecursively()
        }
    }
    return dir
}
