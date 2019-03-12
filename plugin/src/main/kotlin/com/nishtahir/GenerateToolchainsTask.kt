package com.nishtahir

import java.io.File

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

open class GenerateToolchainsTask : DefaultTask() {

    @TaskAction
    @Suppress("unused")
    fun generateToolchainTask() {
        project.plugins.all {
            when (it) {
                is AppPlugin -> configureTask<AppExtension>(project)
                is LibraryPlugin -> configureTask<LibraryExtension>(project)
            }
        }
    }

    inline fun <reified T : BaseExtension> configureTask(project: Project) {
        val cargoExtension = project.extensions[CargoExtension::class]
        val app = project.extensions[T::class]
        val apiLevel = cargoExtension.apiLevel ?: app.defaultConfig.minSdkVersion.apiLevel
        val ndkPath = app.ndkDirectory

        // It's safe to unwrap, since we bailed at configuration time if this is unset.
        val targets = cargoExtension.getLocalTargets(project.name)!!

        toolchains
                .filter { it.type == ToolchainType.ANDROID }
                .filter { (arch) -> targets.contains(arch) }
                .forEach { (arch) ->
                     if (arch.endsWith("64") && apiLevel < 21) {
                        throw GradleException("Can't target 64-bit ${arch} with API level < 21 (${apiLevel})")
                    }

                    val dir = File(cargoExtension.toolchainDirectory, arch + "-" + apiLevel)
                    if (dir.exists()) {
                        println("Toolchain for arch ${arch} version ${apiLevel} exists: checked ${dir}")
                        return@forEach
                    }

                    println("Toolchain for arch ${arch} version ${apiLevel} does not exist: checked ${dir}")
                    project.exec { spec ->
                        spec.standardOutput = System.out
                        spec.errorOutput = System.out
                        spec.commandLine(cargoExtension.pythonCommand)
                        spec.args("$ndkPath/build/tools/make_standalone_toolchain.py",
                                  "--arch=$arch",
                                  "--api=$apiLevel",
                                  "--install-dir=${dir}",
                                  "--force")
                    }
                }
    }
}
