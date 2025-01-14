package com.nishtahir

import java.io.File

class SimpleCargoProject(
    private val projectDir: File,
    private val targets: List<String>
) {
    private val resCargoPath: String = this::class.java.getResource("/rust/Cargo.toml")!!.path

    fun writeProject() {
        val cargoModuleDir = File(resCargoPath).parentFile
        val targetDir = File(cargoModuleDir.parentFile, "target")

        val targetStrings = targets.map { "\"$it\"" }.joinToString { ", " }

        val contents = /*language=kotlin*/ """
            cargo {
                module = "${cargoModuleDir.path.replace('\\', '/')}"
                targetDirectory = "${targetDir.path.replace('\\', '/')}"
                targets = listOf($targetStrings)
                libname = "rust"
            }
        """.trimIndent()

        writeFile("app/build.gradle.kts", contents)
        writeFile("library/build.gradle.kts", contents)
    }

    private fun writeFile(relativePath: String, vararg contents: String) {
        File(projectDir, relativePath).apply {
            parentFile.mkdirs()
            contents.forEach { appendText(it + '\n') }
        }
    }
}