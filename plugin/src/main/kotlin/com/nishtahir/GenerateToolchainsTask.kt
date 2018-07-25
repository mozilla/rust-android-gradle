package com.nishtahir

import java.io.File

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

open class GenerateToolchainsTask : DefaultTask() {

    @TaskAction
    @Suppress("unused")
    fun generateToolchainTask() {
        project.plugins.all {
            when (it) {
                is AppPlugin -> congfigureTask<AppExtension>(project)
                is LibraryPlugin -> congfigureTask<LibraryExtension>(project)
            }
        }
    }

    inline fun <reified T : BaseExtension> congfigureTask(project: Project) {
        val app = project.extensions[T::class]
        val minApi = app.defaultConfig.minSdkVersion.apiLevel
        val ndkPath = app.ndkDirectory

        val targets = project.extensions[CargoExtension::class].targets

        toolchains
                .filterNot { (arch) -> minApi < 21 && arch.endsWith("64") }
                .filter { (arch) -> targets.contains(arch) }
                .forEach { (arch) ->
                    val dir = File(project.getToolchainDirectory(), arch)
                    if (dir.exists()) {
                        println("Toolchain for arch ${arch} exists: checked ${dir}")
                        return@forEach
                    }

                    println("Toolchain for arch ${arch} does not exist: checked ${dir}")
                    project.exec { spec ->
                        spec.standardOutput = System.out
                        spec.errorOutput = System.out
                        spec.commandLine("$ndkPath/build/tools/make_standalone_toolchain.py")
                        spec.args("--arch=$arch", "--api=$minApi",
                                "--install-dir=${project.getToolchainDirectory()}/$arch",
                                "--force")
                    }
                }
    }
}
