package com.nishtahir;

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CargoBuildTask : DefaultTask() {
    var toolchain: Toolchain? = null

    @Suppress("unused")
    @TaskAction
    fun build() = with(project) {
        extensions[CargoExtension::class].apply {
            // Need to capture the value to dereference smoothly.
            val toolchain = toolchain
            if (toolchain == null) {
                throw GradleException("toolchain cannot be null")
            }

            project.plugins.all {
                when (it) {
                    is AppPlugin -> buildProjectForTarget<AppExtension>(project, toolchain, this)
                    is LibraryPlugin -> buildProjectForTarget<LibraryExtension>(project, toolchain, this)
                }
            }

            val targetDirectory = targetDirectory ?: "${module!!}/target"

            copy { spec ->
                if (toolchain.target != null) {
                    spec.from(File(project.projectDir, "${targetDirectory}/${toolchain.target}/${profile}"))
                    spec.into(File(buildDir, "rustJniLibs/${toolchain.folder}"))
                } else {
                    spec.from(File(project.projectDir, "${targetDirectory}/${profile}"))
                    spec.into(File(buildDir, "rustResources/${defaultToolchainBuildPrefixDir}"))
                }

                // Need to capture the value to dereference smoothly.
                val targetIncludes = targetIncludes
                if (targetIncludes != null) {
                    spec.include(targetIncludes.asIterable())
                } else {
                    // It's safe to unwrap, since we bailed at configuration time if this is unset.
                    val libname = libname!!
                    spec.include("${libname}.so")
                    spec.include("${libname}.dylib")
                    spec.include("${libname}.dll")
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
                workingDir = File(project.project.projectDir, cargoExtension.module!!)

                val theCommandLine = mutableListOf("cargo", "build");

                // Respect `verbose` if it is set; otherwise, log if asked to
                // with `--info` or `--debug` from the command line.
                if (cargoExtension.verbose ?: project.logger.isEnabled(LogLevel.INFO)) {
                    theCommandLine.add("--verbose")
                }

                val features = cargoExtension.featureSpec.features
                // We just pass this along to cargo as something space separated... AFAICT
                // you're allowed to have featureSpec with spaces in them, but I don't think
                // there's a way to specify them in the cargo command line -- rustc accepts
                // them if passed in directly with `--cfg`, and cargo will pass them to rustc
                // if you use them as default featureSpec.
                when (features) {
                    is Features.All -> {
                        theCommandLine.add("--all-features")
                    }
                    is Features.DefaultAnd -> {
                        if (!features.featureSet.isEmpty()) {
                            theCommandLine.add("--features")
                            theCommandLine.add(features.featureSet.joinToString(" "))
                        }
                    }
                    is Features.NoDefaultBut -> {
                        theCommandLine.add("--no-default-features")
                        if (!features.featureSet.isEmpty()) {
                            theCommandLine.add("--features")
                            theCommandLine.add(features.featureSet.joinToString(" "))
                        }
                    }
                }

                if (cargoExtension.profile != "debug") {
                    // Cargo is rigid: it accepts "--release" for release (and
                    // nothing for dev).  This is a cheap way of allowing only
                    // two values.
                    theCommandLine.add("--${cargoExtension.profile}")
                }

                if (toolchain.target != null) {
                    theCommandLine.add("--target=${toolchain.target}")

                    // Be aware that RUSTFLAGS can have problems with embedded
                    // spaces, but that shouldn't be a problem here.
                    val cc = File(cargoExtension.toolchainDirectory, "${toolchain.cc(apiLevel)}").path;
                    val ar = File(cargoExtension.toolchainDirectory, "${toolchain.ar(apiLevel)}").path;

                    // For cargo: like "CARGO_TARGET_i686-linux-android_CC".  This is really weakly
                    // documented; see https://github.com/rust-lang/cargo/issues/5690 and follow
                    // links from there.
                    val toolchain_target = toolchain.target.toUpperCase().replace('-', '_')
                    environment("CARGO_TARGET_${toolchain_target}_CC", cc)
                    environment("CARGO_TARGET_${toolchain_target}_AR", ar)

                    // For build.rs in `cc` consumers: like "CC_i686-linux-android".  See
                    // https://github.com/alexcrichton/cc-rs#external-configuration-via-environment-variables.
                    environment("CC_${toolchain.target}", cc)
                    environment("AR_${toolchain.target}", ar)

                    var rustflags = "-C linker=$cc -C link-arg=-Wl,-soname,${cargoExtension.libname!!}.so"
                    environment("RUSTFLAGS", rustflags)
                }

                commandLine = theCommandLine
            }
        }.assertNormalExitValue()
    }
}
