package com.nishtahir

import com.android.build.gradle.AppExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class GenerateToolchainsTask : DefaultTask() {

    @TaskAction
    @Suppress("unused")
    fun generateToolchainTask() {
        val app = project.extensions[AppExtension::class]
        val minApi = app.defaultConfig.minSdkVersion.apiLevel
        val ndkPath = app.ndkDirectory

        if (project.getToolchainDirectory().exists()) {
            println("Existing toolchain found.")
            return
        }

        toolchains
                .filterNot { (arch) -> minApi < 21 && arch.endsWith("64") }
                .forEach { (arch) ->
                    project.exec { spec ->
                        spec.standardOutput = System.out
                        spec.commandLine("$ndkPath/build/tools/make_standalone_toolchain.py")
                        spec.args("--arch=$arch", "--api=$minApi",
                                "--install-dir=${project.getToolchainDirectory()}/$arch")
                    }
                }
    }
}