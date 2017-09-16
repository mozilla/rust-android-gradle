package com.nishtahir;

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CargoBuildTask : DefaultTask() {

    @Suppress("unused")
    @TaskAction
    fun build() = with(project) {
        extensions[CargoExtension::class].apply {
            targets.forEach { target ->
                val toolchain = toolchains.find { (arch) -> arch == target }
                if (toolchain != null) {
                    buildProjectForTarget(project, toolchain, this)

                    copy { spec ->
                        spec.from(File(project.projectDir, "$module/target/${toolchain.target}/debug"))
                        spec.into(File(buildDir, "jniLibs/${toolchain.folder}"))
                        spec.include("*.so")
                    }
                } else {
                    println("No such target $target")
                }
            }
        }
    }

    private fun buildProjectForTarget(project: Project, toolchain: Toolchain, cargoExtension: CargoExtension) {
        project.exec { spec ->
            val CCompiler = "${project.getToolchainDirectory()}/${toolchain.bin()}"
            println("using compiler: $CCompiler")
            spec.standardOutput = System.out
            spec.workingDir = File(project.project.projectDir, cargoExtension.module)
            spec.environment("CC", CCompiler)
            spec.environment("RUSTFLAGS", "-C linker=$CCompiler")
            spec.commandLine = listOf("cargo", "build", "--target=${toolchain.target}")
        }.assertNormalExitValue()
    }
}