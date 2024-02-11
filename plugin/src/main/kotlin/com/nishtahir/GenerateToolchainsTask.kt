package com.nishtahir

import java.io.File

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class GenerateToolchainsTask @Inject constructor(
    private val cargoConfig: CargoConfig,
    private val ndkPath: String,
): DefaultTask() {

    @TaskAction
    @Suppress("unused")
    fun generateToolchainTask() {
        // It's safe to unwrap, since we bailed at configuration time if this is unset.
        val targets = cargoConfig.targets!!

        toolchains
                .filter { it.type == ToolchainType.ANDROID_GENERATED }
                .filter { (arch) -> targets.contains(arch) }
                .forEach { (arch) ->
                     // We ensure all architectures have an API level at configuration time
                     val apiLevel = cargoConfig.apiLevels[arch]!!

                     if (arch.endsWith("64") && apiLevel < 21) {
                        throw GradleException("Can't target 64-bit $arch with API level < 21 (${apiLevel})")
                    }

                    // Always regenerate the toolchain, even if it exists
                    // already. It is fast to do so and fixes any issues
                    // with partially reclaimed temporary files.
                    val dir = File(cargoConfig.toolchainDirectory, "$arch-$apiLevel")
                    project.exec { spec ->
                        spec.standardOutput = System.out
                        spec.errorOutput = System.out
                        spec.commandLine(cargoConfig.pythonCommand)
                        spec.args("$ndkPath/build/tools/make_standalone_toolchain.py",
                                  "--arch=$arch",
                                  "--api=$apiLevel",
                                  "--install-dir=${dir}",
                                  "--force")
                    }
                }
    }
}
