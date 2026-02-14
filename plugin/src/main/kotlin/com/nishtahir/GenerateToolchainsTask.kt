package com.nishtahir

import java.io.File

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class GenerateToolchainsTask : DefaultTask() {

    @get:Inject
    abstract val execOperations: ExecOperations

    @Input
    var targets: List<String> = listOf()

    @Input
    var apiLevels: Map<String, Int> = mapOf()

    @Input
    var pythonCommand: String = "python"

    @Internal
    var toolchainDirectory: File = File("")

    @Internal
    var ndkDirectory: File = File("")

    @TaskAction
    @Suppress("unused")
    fun generateToolchainTask() {
        toolchains
                .filter { it.type == ToolchainType.ANDROID_GENERATED }
                .filter { (arch) -> targets.contains(arch) }
                .forEach { (arch) ->
                     // We ensure all architectures have an API level at configuration time
                     val apiLevel = apiLevels[arch]!!

                     if (arch.endsWith("64") && apiLevel < 21) {
                        throw GradleException("Can't target 64-bit ${arch} with API level < 21 (${apiLevel})")
                    }

                    // Always regenerate the toolchain, even if it exists
                    // already. It is fast to do so and fixes any issues
                    // with partially reclaimed temporary files.
                    val dir = File(toolchainDirectory, arch + "-" + apiLevel)
                    execOperations.exec { spec ->
                        spec.standardOutput = System.out
                        spec.errorOutput = System.out
                        spec.commandLine(pythonCommand)
                        spec.args("$ndkDirectory/build/tools/make_standalone_toolchain.py",
                                  "--arch=$arch",
                                  "--api=$apiLevel",
                                  "--install-dir=${dir}",
                                  "--force")
                    }
                }
    }
}
