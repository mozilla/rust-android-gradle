package com.nishtahir;

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.process.ExecSpec
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class CargoBuildTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @Input
    var toolchain: Toolchain? = null

    @Input
    var ndk: Ndk? = null

    @Internal
    var projectDir: File = File("")

    @Internal
    var buildDir: File = File("")

    @Internal
    var rootBuildDir: File = File("")

    @Input
    var cargoCommand: String = "cargo"

    @Input
    var rustcCommand: String = "rustc"

    @Input
    var rustupChannel: String = ""

    @Input
    var pythonCommand: String = "python"

    @Input
    var module: String = ""

    @Input
    @Optional
    var libname: String? = null

    @Input
    @Optional
    var verbose: Boolean? = null

    @Input
    var profile: String = "debug"

    @Input
    @Optional
    var cargoTargetDir: String? = null

    @Input
    @Optional
    var targetIncludes: Array<String>? = null

    @Input
    var featureSpec: FeatureSpec = FeatureSpec()

    @Input
    @Optional
    var extraCargoBuildArguments: List<String>? = null

    @Input
    var apiLevels: Map<String, Int> = mapOf()

    @Input
    var generateBuildId: Boolean = false

    @Internal
    var toolchainDirectory: File = File("")

    @Input
    var autoConfigureClangSys: Boolean = false

    @Input
    var targetProperties: Map<String, String> = mapOf()

    @Internal
    var execClosure: ((ExecSpec, Toolchain) -> Unit)? = null

    @Suppress("unused")
    @TaskAction
    fun build() {
        val toolchain = toolchain ?: throw GradleException("toolchain cannot be null")
        val ndk = ndk ?: throw GradleException("ndk cannot be null")

        val apiLevel = apiLevels[toolchain.platform]!!
        val defaultTargetTriple = getDefaultTargetTriple(execOperations, logger, rustcCommand)

        execOperations.exec { spec ->
            with(spec) {
                standardOutput = System.out
                val moduleFile = File(module)
                if (moduleFile.isAbsolute) {
                    workingDir = moduleFile
                } else {
                    workingDir = File(projectDir, moduleFile.path)
                }
                workingDir = workingDir.canonicalFile

                val theCommandLine = mutableListOf(cargoCommand)

                if (!rustupChannel.isEmpty()) {
                    val hasPlusSign = rustupChannel.startsWith("+")
                    val maybePlusSign = if (!hasPlusSign) "+" else ""

                    theCommandLine.add(maybePlusSign + rustupChannel)
                }

                theCommandLine.add("build")

                // Respect `verbose` if it is set; otherwise, log if asked to
                // with `--info` or `--debug` from the command line.
                if (verbose ?: logger.isEnabled(LogLevel.INFO)) {
                    theCommandLine.add("--verbose")
                }

                val features = featureSpec.features
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

                if (profile != "debug") {
                    // Cargo is rigid: it accepts "--release" for release (and
                    // nothing for dev).  This is a cheap way of allowing only
                    // two values.
                    theCommandLine.add("--${profile}")
                }
                if (toolchain.target != defaultTargetTriple) {
                    // Only providing --target for the non-default targets means desktop builds
                    // can share the build cache with `cargo build`/`cargo test`/etc invocations,
                    // instead of requiring a large amount of redundant work.
                    theCommandLine.add("--target=${toolchain.target}")
                }

                // Target-specific environment configuration, passed through to
                // the underlying `cargo build` invocation.
                val toolchain_target = toolchain.target.toUpperCase().replace('-', '_')
                val prefix = "RUST_ANDROID_GRADLE_TARGET_${toolchain_target}_"

                // For ORG_GRADLE_PROJECT_RUST_ANDROID_GRADLE_TARGET_x_KEY=VALUE, set KEY=VALUE.
                logger.info("Passing through project properties with prefix '${prefix}' (environment variables with prefix 'ORG_GRADLE_PROJECT_${prefix}'")
                targetProperties.forEach { (key, value) ->
                                         if (key.startsWith(prefix)) {
                                             val realKey = key.substring(prefix.length)
                                             logger.debug("Passing through environment variable '${key}' as '${realKey}=${value}'")
                                             environment(realKey, value)
                                         }
                }

                // Cross-compiling to Android requires toolchain massaging.
                if (toolchain.type != ToolchainType.DESKTOP) {
                    val ndkPath = ndk.path
                    val ndkVersionMajor = ndk.versionMajor

                    val toolchainDir = if (toolchain.type == ToolchainType.ANDROID_PREBUILT) {
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
                        File("$ndkPath/toolchains/llvm/prebuilt", hostTag)
                    } else {
                        toolchainDirectory
                    }

                    val linker_wrapper =
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        File(rootBuildDir, "linker-wrapper/linker-wrapper.bat")
                    } else {
                        File(rootBuildDir, "linker-wrapper/linker-wrapper.sh")
                    }
                    environment("CARGO_TARGET_${toolchain_target}_LINKER", linker_wrapper.path)

                    val cc = File(toolchainDir, "${toolchain.cc(apiLevel)}").path;
                    val cxx = File(toolchainDir, "${toolchain.cxx(apiLevel)}").path;
                    val ar = File(toolchainDir, "${toolchain.ar(apiLevel, ndkVersionMajor)}").path;

                    // For build.rs in `cc` consumers: like "CC_i686-linux-android".  See
                    // https://github.com/alexcrichton/cc-rs#external-configuration-via-environment-variables.
                    environment("CC_${toolchain.target}", cc)
                    environment("CXX_${toolchain.target}", cxx)
                    environment("AR_${toolchain.target}", ar)

                    // Set CLANG_PATH in the environment, so that bindgen (or anything
                    // else using clang-sys in a build.rs) works properly, and doesn't
                    // use host headers and such.
                    if (autoConfigureClangSys) {
                        environment("CLANG_PATH", cc)
                    }

                    // Configure our linker wrapper.
                    environment("RUST_ANDROID_GRADLE_PYTHON_COMMAND", pythonCommand)
                    environment("RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY",
                            File(rootBuildDir, "linker-wrapper/linker-wrapper.py").path)
                    environment("RUST_ANDROID_GRADLE_CC", cc)
                    if (generateBuildId) {
                        environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,--build-id,-soname,lib${libname!!}.so")
                    } else {
                        environment("RUST_ANDROID_GRADLE_CC_LINK_ARG", "-Wl,-soname,lib${libname!!}.so")
                    }
                }

                extraCargoBuildArguments?.let {
                    theCommandLine.addAll(it)
                }

                commandLine = theCommandLine
            }
            if (execClosure != null) {
                (execClosure!!)(spec, toolchain)
            }
        }.assertNormalExitValue()

        // CARGO_TARGET_DIR can be used to force the use of a global, shared target directory
        // across all rust projects on a machine. Use it if it's set, otherwise use the
        // configured `targetDirectory` value, and fall back to `${module}/target`.
        val target = cargoTargetDir ?: "${module}/target"

        var cargoOutputDir = File(if (toolchain.target == defaultTargetTriple) {
            "${target}/${profile}"
        } else {
            "${target}/${toolchain.target}/${profile}"
        })
        if (!cargoOutputDir.isAbsolute) {
            cargoOutputDir = File(projectDir, cargoOutputDir.path)
        }
        cargoOutputDir = cargoOutputDir.canonicalFile

        val intoDir = File(buildDir, "rustJniLibs/${toolchain.folder}")
        intoDir.mkdirs()

        fileSystemOperations.copy { spec ->
            spec.from(cargoOutputDir)
            spec.into(intoDir)

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

fun getDefaultTargetTriple(execOperations: ExecOperations, logger: org.gradle.api.logging.Logger, rustc: String): String? {
    val stdout = ByteArrayOutputStream()
    val result = execOperations.exec { spec ->
        spec.standardOutput = stdout
        spec.commandLine = listOf(rustc, "--version", "--verbose")
    }
    if (result.exitValue != 0) {
        logger.warn(
            "Failed to get default target triple from rustc (exit code: ${result.exitValue})")
        return null
    }
    val output = stdout.toString()

    // The `rustc --version --verbose` output contains a number of lines like `key: value`.
    // We're only interested in `host: `, which corresponds to the default target triple.
    val triplePrefix = "host: "

    val triple = output.split("\n")
        .find { it.startsWith(triplePrefix) }
        ?.let { it.substring(triplePrefix.length).trim() }

    if (triple == null) {
        logger.warn("Failed to parse `rustc -Vv` output! (Please report a rust-android-gradle bug)")
    } else {
        logger.info("Default rust target triple: $triple")
    }
    return triple
}
