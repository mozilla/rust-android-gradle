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
            val cc = "${project.getToolchainDirectory()}/${toolchain.cc()}"
            val ar = "${project.getToolchainDirectory()}/${toolchain.ar()}"
            println("using CC: $cc")
            println("using AR: $ar")
            with(spec) {
                standardOutput = System.out
                workingDir = File(project.project.projectDir, cargoExtension.module)
                environment("CC", cc)
                environment("AR", ar)
                environment("RUSTFLAGS", "-C linker=$cc")
                commandLine = listOf("cargo", "build", "--target=${toolchain.target}")
            }
        }.assertNormalExitValue()
    }
}