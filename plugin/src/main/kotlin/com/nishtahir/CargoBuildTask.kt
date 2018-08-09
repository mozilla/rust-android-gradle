package com.nishtahir;

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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

                if (toolchain == null) {
                    throw GradleException("No such target $target")
                }

                project.plugins.all {
                    when (it) {
                        is AppPlugin -> buildProjectForTarget<AppExtension>(project, toolchain, this)
                        is LibraryPlugin -> buildProjectForTarget<LibraryExtension>(project, toolchain, this)
                    }
                }

                val targetDirectory = targetDirectory ?: "${module}/target"

                copy { spec ->
                    if (toolchain.target != null) {
                        spec.from(File(project.projectDir, "${targetDirectory}/${toolchain.target}/${profile}"))
                        spec.into(File(buildDir, "rustJniLibs/${toolchain.folder}"))
                    } else {
                        spec.from(File(project.projectDir, "${targetDirectory}/${profile}"))
                        spec.into(File(buildDir, "rustResources/${defaultToolchainBuildPrefixDir}"))
                    }
                    spec.include(targetIncludes.asIterable())
                }
            }
        }
    }

    inline fun <reified T : BaseExtension> buildProjectForTarget(project: Project, toolchain: Toolchain, cargoExtension: CargoExtension) {
        val app = project.extensions[T::class]
        val apiLevel = cargoExtension.apiLevel ?: app.defaultConfig.minSdkVersion.apiLevel

        project.exec { spec ->
            if (cargoExtension.exec != null) {
                (cargoExtension.exec!!)(spec, toolchain)
            }

            with(spec) {
                standardOutput = System.out
                workingDir = File(project.project.projectDir, cargoExtension.module)

                val theCommandLine = mutableListOf("cargo", "build");

                if (cargoExtension.profile != "debug") {
                    // Cargo is rigid: it accepts "--release" for release (and
                    // nothing for dev).  This is a cheap way of allowing only
                    // two values.
                    theCommandLine.add("--${cargoExtension.profile}")
                }

                if (toolchain.target != null) {
                    theCommandLine.add("--target=${toolchain.target}")

                    val cc = "${project.getToolchainDirectory()}/${toolchain.cc(apiLevel)}"
                    val ar = "${project.getToolchainDirectory()}/${toolchain.ar(apiLevel)}"
                    environment("CC", cc)
                    environment("AR", ar)
                    environment("RUSTFLAGS", "-C linker=$cc")
                }

                commandLine = theCommandLine
            }
        }.assertNormalExitValue()
    }
}
