package com.nishtahir

import com.android.build.gradle.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputFiles
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.inject.Inject


open class CargoBuildTask @Inject constructor(
    private val toolchain: Toolchain,
    private val cargoConfig: CargoConfig,
): DefaultTask() {
    @get:InputFiles
    lateinit var manifestDir: ConfigurableFileTree

    @get:OutputDirectory
    lateinit var destinationDir: File

    @TaskAction
    fun taskAction() {
        project.plugins.all {
            when (it) {
                is AppPlugin -> {
                    val ndkDirectory = project.extensions[AppExtension::class].ndkDirectory
                    buildProjectForTarget(project, toolchain, cargoConfig, ndkDirectory)
                }
                is LibraryPlugin -> {
                    val ndkDirectory = project.extensions[LibraryExtension::class].ndkDirectory
                    buildProjectForTarget(project, toolchain, cargoConfig, ndkDirectory)
                }
            }
        }

        val defaultTargetTriple = getDefaultTargetTriple(project, cargoConfig.rustcCommand)

        // cargo.profile is non-null here
        val targetDirectoryProfile = getTargetDirectoryFromProfile(cargoConfig.profile!!)

        val cargoOutputDir = project.file(if (toolchain.target == defaultTargetTriple) {
            "${cargoConfig.targetDirectory}/${targetDirectoryProfile}"
        } else {
            "${cargoConfig.targetDirectory}/${toolchain.target}/${targetDirectoryProfile}"
        })

        destinationDir.mkdirs()

        project.copy { spec ->
            spec.from(cargoOutputDir)
            spec.into(destinationDir)

            // Need to capture the value to dereference smoothly.
            val targetIncludes = cargoConfig.targetIncludes
            if (targetIncludes != null) {
                spec.include(targetIncludes.asIterable())
            } else {
                // It's safe to unwrap, since we bailed at configuration time if this is unset.
                val libName = cargoConfig.libname!!
                spec.include("lib${libName}.so")
                spec.include("lib${libName}.dylib")
                spec.include("${libName}.dll")
            }
        }
    }

    private fun buildProjectForTarget(project: Project, toolchain: Toolchain, cargoConfig: CargoConfig, ndkDirectory: File) {
        val apiLevel = cargoConfig.apiLevels[toolchain.platform]!!
        val defaultTargetTriple = getDefaultTargetTriple(project, cargoConfig.rustcCommand)

        project.exec {
            with(it) {
                standardOutput = System.out
                workingDir = manifestDir.dir

                project.logger.info("Target directory: ${cargoConfig.targetDirectory}")
                if (cargoConfig.targetDirectory != null) {
                    environment("CARGO_TARGET_DIR", project.file(cargoConfig.targetDirectory!!))
                }

                val theCommandLine = mutableListOf(cargoConfig.cargoCommand)

                if (cargoConfig.rustupChannel.isNotEmpty()) {
                    val hasPlusSign = cargoConfig.rustupChannel.startsWith("+")
                    val maybePlusSign = if (!hasPlusSign) "+" else ""

                    theCommandLine.add(maybePlusSign + cargoConfig.rustupChannel)
                }

                theCommandLine.add("build")

                // Respect `verbose` if it is set; otherwise, log if asked to
                // with `--info` or `--debug` from the command line.
                if (cargoConfig.verbose ?: project.logger.isEnabled(LogLevel.DEBUG)) {
                    theCommandLine.add("--verbose")
                } else if (project.logger.isEnabled(LogLevel.LIFECYCLE)) {
                    theCommandLine.add("--quiet")
                }

                // We just pass this along to cargo as something space separated... AFAICT
                // you're allowed to have featureSpec with spaces in them, but I don't think
                // there's a way to specify them in the cargo command line -- rustc accepts
                // them if passed in directly with `--cfg`, and cargo will pass them to rustc
                // if you use them as default featureSpec.
                when (val features = cargoConfig.featureSpec.features) {
                    is Features.All -> {
                        theCommandLine.add("--all-features")
                    }
                    is Features.DefaultAnd -> {
                        if (features.featureSet.isNotEmpty()) {
                            theCommandLine.add("--features")
                            theCommandLine.add(features.featureSet.joinToString(" "))
                        }
                    }
                    is Features.NoDefaultBut -> {
                        theCommandLine.add("--no-default-features")
                        if (features.featureSet.isNotEmpty()) {
                            theCommandLine.add("--features")
                            theCommandLine.add(features.featureSet.joinToString(" "))
                        }
                    }
                    null -> {}
                }

                theCommandLine.add("--profile=${cargoConfig.profile}")

                if (toolchain.target != defaultTargetTriple) {
                    // Only providing --target for the non-default targets means desktop builds
                    // can share the build cache with `cargo build`/`cargo test`/etc invocations,
                    // instead of requiring a large amount of redundant work.
                    theCommandLine.add("--target=${toolchain.target}")
                }

                // Target-specific environment configuration, passed through to
                // the underlying `cargo build` invocation.
                val toolchainTarget = toolchain.target
                    .uppercase(Locale.getDefault())
                    .replace('-', '_')
                val prefix = "RUST_ANDROID_GRADLE_TARGET_${toolchainTarget}_"

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
                if (toolchain.type != ToolchainType.DESKTOP) {
                    val ndkVersion = ndkDirectory.name
                    val ndkVersionMajor = try {
                        ndkVersion.split(".").first().toInt()
                    } catch (ex: NumberFormatException) {
                        0 // Falls back to generic behaviour.
                    }

                    val toolchainDirectory = if (toolchain.type == ToolchainType.ANDROID_PREBUILT) {
                        environment("CARGO_NDK_MAJOR_VERSION", ndkVersionMajor)

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
                        File("$ndkDirectory/toolchains/llvm/prebuilt", hostTag)
                    } else {
                        cargoConfig.toolchainDirectory
                    }

                    val linkerWrapper =
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.bat")
                    } else {
                        File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.sh")
                    }
                    environment("CARGO_TARGET_${toolchainTarget}_LINKER", linkerWrapper.path)

                    val cc = File(toolchainDirectory, "${toolchain.cc(apiLevel)}").path
                    val cxx = File(toolchainDirectory, "${toolchain.cxx(apiLevel)}").path
                    val ar = File(toolchainDirectory, "${toolchain.ar(apiLevel, ndkVersionMajor)}").path

                    // For build.rs in `cc` consumers: like "CC_i686-linux-android".  See
                    // https://github.com/alexcrichton/cc-rs#external-configuration-via-environment-variables.
                    environment("CC_${toolchain.target}", cc)
                    environment("CXX_${toolchain.target}", cxx)
                    environment("AR_${toolchain.target}", ar)

                    // Set CLANG_PATH in the environment, so that bindgen (or anything
                    // else using clang-sys in a build.rs) works properly, and doesn't
                    // use host headers and such.
                    val shouldConfigure = cargoConfig.getFlagProperty(
                        "rust.autoConfigureClangSys",
                        "RUST_ANDROID_GRADLE_AUTO_CONFIGURE_CLANG_SYS",
                        // By default, only do this for non-desktop platforms. If we're
                        // building for desktop, things should work out of the box.
                        toolchain.type != ToolchainType.DESKTOP
                    )
                    if (shouldConfigure) {
                        environment("CLANG_PATH", cc)
                    }

                    // Configure our linker wrapper.
                    environment("RUST_ANDROID_GRADLE_PYTHON_COMMAND", cargoConfig.pythonCommand)
                    environment("RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY",
                            File(project.rootProject.buildDir, "linker-wrapper/linker-wrapper.py").path)
                    environment("RUST_ANDROID_GRADLE_CC", cc)
                    environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,-soname,lib${cargoConfig.libname!!}.so")
                }

                cargoConfig.extraCargoBuildArguments?.let { args ->
                    theCommandLine.addAll(args)
                }

                project.logger.info("cargo command: $theCommandLine")
                commandLine = theCommandLine
            }
            if (cargoConfig.exec != null) {
                (cargoConfig.exec!!)(it, toolchain)
            }
        }.assertNormalExitValue()
    }
}

// This can't be private/internal as it's called from `buildProjectForTarget`.
fun getDefaultTargetTriple(project: Project, rustc: String): String? {
    val stdout = ByteArrayOutputStream()
    val result = project.exec { spec ->
        spec.standardOutput = stdout
        spec.commandLine = listOf(rustc, "--version", "--verbose")
    }
    if (result.exitValue != 0) {
        project.logger.warn(
            "Failed to get default target triple from rustc (exit code: ${result.exitValue})")
        return null
    }
    val output = stdout.toString()

    // The `rustc --version --verbose` output contains a number of lines like `key: value`.
    // We're only interested in `host: `, which corresponds to the default target triple.
    val triplePrefix = "host: "

    val triple = output.split("\n")
        .find { it.startsWith(triplePrefix) }
        ?.substring(triplePrefix.length)
        ?.trim()

    if (triple == null) {
        project.logger.warn("Failed to parse `rustc -Vv` output! (Please report a rust-android-gradle bug)")
    } else {
        project.logger.info("Default rust target triple: $triple")
    }
    return triple
}

fun getTargetDirectoryFromProfile(profile: String): String {
    return when (profile) {
        "dev" -> "debug"
        else -> profile
    }
}
