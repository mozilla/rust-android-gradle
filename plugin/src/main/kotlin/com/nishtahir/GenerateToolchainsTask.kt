package com.nishtahir

import java.io.File

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GenerateToolchainsTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

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
        val ndkPath = app.ndkDirectory

        // It's safe to unwrap, since we bailed at configuration time if this is unset.
        val targets = cargoExtension.targets!!

        toolchains
            .filter { it.type == ToolchainType.ANDROID_GENERATED }
            .filter { (arch) -> targets.contains(arch) }
            .forEach { (arch) ->
                 // We ensure all architectures have an API level at configuration time
                 val apiLevel = cargoExtension.apiLevels[arch]!!

                 if (arch.endsWith("64") && apiLevel < 21) {
                    throw GradleException("Can't target 64-bit $arch with API level < 21 (${apiLevel})")
                }

                // Always regenerate the toolchain, even if it exists
                // already. It is fast to do so and fixes any issues
                // with partially reclaimed temporary files.
                val dir = File(cargoExtension.toolchainDirectory, "$arch-$apiLevel")
                execOperations.exec { spec ->
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
