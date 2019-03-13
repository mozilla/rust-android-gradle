package com.nishtahir;

import com.android.build.gradle.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import java.io.File

open class CargoBuildTask : DefaultTask() {
    var toolchain: Toolchain? = null
    var prebuilt: Boolean = false

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
                spec.from(File(project.projectDir, "${targetDirectory}/${toolchain.target}/${profile}"))
                spec.into(File(buildDir, "rustJniLibs/${toolchain.folder}"))

                // Need to capture the value to dereference smoothly.
                val targetIncludes = targetIncludes
                if (targetIncludes != null) {
                    spec.include(targetIncludes.asIterable())
                } else {
                    // It's safe to unwrap, since we bailed at configuration time if this is unset.
                    val libname = libname!!
                    spec.include("lib${libname}.so")
                    spec.include("lib${libname}.dylib")
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

                val theCommandLine = mutableListOf(cargoExtension.cargoCommand, "build");

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

                theCommandLine.add("--target=${toolchain.target}")

                // Target-specific environment configuration, passed through to
                // the underlying `cargo build` invocation.
                val toolchain_target = toolchain.target.toUpperCase().replace('-', '_')
                val prefix = "RUST_ANDROID_GRADLE_TARGET_${toolchain_target}_"

                // For ORG_GRADLE_PROJECT_RUST_ANDROID_GRADLE_TARGET_x_KEY=VALUE, set KEY=VALUE.
                project.logger.info("Passing through project properties with prefix '${prefix}' (environment variables with prefix 'ORG_GRADLE_PROJECT_${prefix}'")
                project.properties.forEach { (key, value) ->
                                             if (key.startsWith(prefix)) {
                                                 val realKey = key.substring(prefix.length)
                                                 project.logger.debug("Passing through environment variable '${key}' as '${realKey}=${value}'")
                                                 environment(realKey, value)
                                             }
                }

                // Cross-compiling to Android requires toolchain massaging.
                if (toolchain.type == ToolchainType.ANDROID) {
                    val toolchainDirectory = if (prebuilt) {
                        val ndkPath = app.ndkDirectory
                        val hostTag = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                            if (Os.isArch("x86_64") || Os.isArch("amd64")) {
                                "windows-x86_64"
                            } else {
                                "windows"
                            }
                        } else if (Os.isFamily(Os.FAMILY_MAC)) {
                            "darwin-x86_64"
                        } else {
                            "linux-x86_64"
                        }
                        File("$ndkPath/toolchains/llvm/prebuilt", hostTag)
                    } else {
                        cargoExtension.toolchainDirectory
                    }

                    // Be aware that RUSTFLAGS can have problems with embedded
                    // spaces, but that shouldn't be a problem here.
                    val cc = File(toolchainDirectory, "${toolchain.cc(prebuilt, apiLevel)}").path;
                    val ar = File(toolchainDirectory, "${toolchain.ar(prebuilt, apiLevel)}").path;

                    // For cargo: like "CARGO_TARGET_I686_LINUX_ANDROID_CC".  This is really weakly
                    // documented; see https://github.com/rust-lang/cargo/issues/5690 and follow
                    // links from there.
                    environment("CARGO_TARGET_${toolchain_target}_CC", cc)
                    environment("CARGO_TARGET_${toolchain_target}_AR", ar)

                    val linker_wrapper =
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.bat")
                    } else {
                        File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.sh")
                    }
                    environment("CARGO_TARGET_${toolchain_target}_LINKER", linker_wrapper.path)

                    // For build.rs in `cc` consumers: like "CC_i686-linux-android".  See
                    // https://github.com/alexcrichton/cc-rs#external-configuration-via-environment-variables.
                    environment("CC_${toolchain.target}", cc)
                    environment("AR_${toolchain.target}", ar)

                    // Configure our linker wrapper.
                    environment("RUST_ANDROID_GRADLE_PYTHON_COMMAND", cargoExtension.pythonCommand)
                    environment("RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY",
                            File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.py").path)
                    environment("RUST_ANDROID_GRADLE_CC", cc)
                    environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,-soname,lib${cargoExtension.libname!!}.so")
                }

                commandLine = theCommandLine
            }
        }.assertNormalExitValue()
    }
}
