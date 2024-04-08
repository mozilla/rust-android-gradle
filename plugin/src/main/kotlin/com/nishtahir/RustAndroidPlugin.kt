package com.nishtahir

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.*
import org.gradle.api.*
import org.gradle.api.file.DuplicatesStrategy
import java.io.File
import java.util.*

const val RUST_TASK_GROUP = "rust"

enum class ToolchainType {
    ANDROID_PREBUILT,
    ANDROID_GENERATED,
    DESKTOP,
}

// See https://forge.rust-lang.org/platform-support.html.
val toolchains = listOf(
        Toolchain("linux-x86-64",
                ToolchainType.DESKTOP,
                "x86_64-unknown-linux-gnu",
                "<compilerTriple>",
                "<binutilsTriple>",
                "linux-x86-64"),
        // This should eventually go away: the darwin-x86-64 target will supersede it.
        // https://github.com/mozilla/rust-android-gradle/issues/77
        Toolchain("darwin",
                ToolchainType.DESKTOP,
                "x86_64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "darwin"),
        Toolchain("darwin-x86-64",
                ToolchainType.DESKTOP,
                "x86_64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "darwin-x86-64"),
        Toolchain("darwin-aarch64",
                ToolchainType.DESKTOP,
                "aarch64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "darwin-aarch64"),
        Toolchain("win32-x86-64-msvc",
                ToolchainType.DESKTOP,
                "x86_64-pc-windows-msvc",
                "<compilerTriple>",
                "<binutilsTriple>",
                "win32-x86-64"),
        Toolchain("win32-x86-64-gnu",
                ToolchainType.DESKTOP,
                "x86_64-pc-windows-gnu",
                "<compilerTriple>",
                "<binutilsTriple>",
                "win32-x86-64"),
        Toolchain("arm",
                ToolchainType.ANDROID_GENERATED,
                "armv7-linux-androideabi",
                "arm-linux-androideabi",
                "arm-linux-androideabi",
                "armeabi-v7a"),
        Toolchain("arm64",
                ToolchainType.ANDROID_GENERATED,
                "aarch64-linux-android",
                "aarch64-linux-android",
                "aarch64-linux-android",
                "arm64-v8a"),
        Toolchain("x86",
                ToolchainType.ANDROID_GENERATED,
                "i686-linux-android",
                "i686-linux-android",
                "i686-linux-android",
                "x86"),
        Toolchain("x86_64",
                ToolchainType.ANDROID_GENERATED,
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64"),
        Toolchain("arm",
                ToolchainType.ANDROID_PREBUILT,
                "armv7-linux-androideabi",  // This is correct.  "Note: For 32-bit ARM, the compiler is prefixed with
                "armv7a-linux-androideabi", // armv7a-linux-androideabi, but the binutils tools are prefixed with
                "arm-linux-androideabi",    // arm-linux-androideabi. For other architectures, the prefixes are the same
                "armeabi-v7a"),     // for all tools."  (Ref: https://developer.android.com/ndk/guides/other_build_systems#overview )
        Toolchain("arm64",
                ToolchainType.ANDROID_PREBUILT,
                "aarch64-linux-android",
                "aarch64-linux-android",
                "aarch64-linux-android",
                "arm64-v8a"),
        Toolchain("x86",
                ToolchainType.ANDROID_PREBUILT,
                "i686-linux-android",
                "i686-linux-android",
                "i686-linux-android",
                "x86"),
        Toolchain("x86_64",
                ToolchainType.ANDROID_PREBUILT,
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64")
)

data class Toolchain(val platform: String,
                     val type: ToolchainType,
                     val target: String,
                     val compilerTriple: String,
                     val binutilsTriple: String,
                     val folder: String) {
    fun cc(apiLevel: Int): File =
            if (System.getProperty("os.name").startsWith("Windows")) {
                if (type == ToolchainType.ANDROID_PREBUILT) {
                    File("bin", "$compilerTriple$apiLevel-clang.cmd")
                } else {
                    File("$platform-$apiLevel/bin", "$compilerTriple-clang.cmd")
                }
            } else {
                if (type == ToolchainType.ANDROID_PREBUILT) {
                    File("bin", "$compilerTriple$apiLevel-clang")
                } else {
                    File("$platform-$apiLevel/bin", "$compilerTriple-clang")
                }
            }

    fun cxx(apiLevel: Int): File =
            if (System.getProperty("os.name").startsWith("Windows")) {
                if (type == ToolchainType.ANDROID_PREBUILT) {
                    File("bin", "$compilerTriple$apiLevel-clang++.cmd")
                } else {
                    File("$platform-$apiLevel/bin", "$compilerTriple-clang++.cmd")
                }
            } else {
                if (type == ToolchainType.ANDROID_PREBUILT) {
                    File("bin", "$compilerTriple$apiLevel-clang++")
                } else {
                    File("$platform-$apiLevel/bin", "$compilerTriple-clang++")
                }
            }

    fun ar(apiLevel: Int, ndkVersionMajor: Int): File =
            if (ndkVersionMajor >= 23) {
                File("bin", "llvm-ar")
            } else if (type == ToolchainType.ANDROID_PREBUILT) {
                File("bin", "$binutilsTriple-ar")
            } else {
                File("$platform-$apiLevel/bin", "$binutilsTriple-ar")
            }
}

@Suppress("unused")
open class RustAndroidPlugin : Plugin<Project> {
    private lateinit var cargoExtension: CargoExtension

    private val androidBuildTask = "cargoBuildAndroid"
    private val hostBuildTask = "cargoBuildHost"
    private val generateLinkerWrapperTask = "generateLinkerWrapper"

    override fun apply(project: Project) {
        with(project) {
            cargoExtension = extensions.create("cargo", CargoExtension::class.java, this)

            // Fish linker wrapper scripts from our Java resources.
            tasks.register(generateLinkerWrapperTask, GenerateLinkerWrapperTask::class.java).configure {
                it.group = RUST_TASK_GROUP
                it.description = "Generate shared linker wrapper script"

                with(it) {
                    // From https://stackoverflow.com/a/320595.
                    from(rootProject.zipTree(File(RustAndroidPlugin::class.java.protectionDomain.codeSource.location.toURI()).path))
                    include("**/linker-wrapper*")
                    into(File(rootProject.buildDir, "linker-wrapper"))
                    eachFile { file ->
                        file.path = file.path.replaceFirst("com/nishtahir", "")
                    }
                    fileMode = 493 // 0755 in decimal; Kotlin doesn't have octal literals (!).
                    includeEmptyDirs = false
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                }
            }

            afterEvaluate {
                plugins.all {
                    when (it) {
                        is AppPlugin -> configurePlugin<AppExtension>(this)
                        is LibraryPlugin -> configurePlugin<LibraryExtension>(this)
                    }
                }
            }

        }
    }

    private inline fun <reified T : BaseExtension> configurePlugin(project: Project) = with(project) {
        tasks.register(androidBuildTask, DefaultTask::class.java) {
            it.group = RUST_TASK_GROUP
            it.description = "Build library for android"
        }

        tasks.register(hostBuildTask, DefaultTask::class.java) {
            it.group = RUST_TASK_GROUP
            it.description = "Build library for host"
        }

        when (val androidExtension = extensions[T::class]) {
            is AppExtension -> androidExtension.applicationVariants.all { variant -> 
                configureForVariant<T>(project, variant)
            }
            is LibraryExtension -> androidExtension.libraryVariants.all { variant ->
                configureForVariant<T>(project, variant)
            }
        }
    }

    private inline fun <reified T: BaseExtension> configureForVariant(project: Project, variant: BaseVariant) = with(project) {
        val capitalisedVariantName = variant.name.replaceFirstChar { it.uppercase() }
        val config = cargoExtension.getConfig(variant)

        config.localProperties = Properties()

        val localPropertiesFile = File(project.rootDir, "local.properties")
        if (localPropertiesFile.exists()) {
            config.localProperties.load(localPropertiesFile.inputStream())
        }

        if (config.module == null) {
            throw GradleException("module cannot be null")
        }

        if (config.libname == null) {
            throw GradleException("libname cannot be null")
        }

        config.profile = config.profile ?: (if (variant.buildType.isDebuggable) { "dev" } else { "release" })

        // Allow to set targets, including per-project, in local.properties.
        val localTargets: String? =
                config.localProperties.getProperty("rust.targets.${project.name}") ?:
                config.localProperties.getProperty("rust.targets")
        if (config.targets == null && localTargets != null) {
            config.targets = localTargets.split(',').map { it.trim() }
        }

        if (config.targets == null) {
            throw GradleException("targets cannot be null")
        }

        // Ensure that an API level is specified for all targets
        val apiLevel = config.apiLevel
        if (config.apiLevels.isNotEmpty()) {
            if (apiLevel != null) {
                throw GradleException("Cannot set both `apiLevel` and `apiLevels`")
            }
        } else {
            val default = apiLevel ?: extensions[T::class].defaultConfig.minSdkVersion!!.apiLevel
            config.apiLevels = config.targets!!.associateWith { default }
        }
        val missingApiLevelTargets = config.targets!!.toSet().minus(
            config.apiLevels.keys)
        if (missingApiLevelTargets.isNotEmpty()) {
            throw GradleException("`apiLevels` missing entries for: $missingApiLevelTargets")
        }

        // Cargo's target directory is set by the following precedence:
        // 1. The `targetDirectory` property in the `CargoExtension` block.
        // 2. The `rust.cargoTargetDir` property in the `local.properties` file.
        // 3. The `CARGO_TARGET_DIR` environment variable.
        // 4. The default `${module}/target` directory.
        //
        // We allow this to be specified in `local.properties`, not because this is
        // something you should ever need to do currently, but we don't want it to ruin anyone's
        // day if it turns out we're wrong about that.
        config.targetDirectory = config.targetDirectory
            ?: config.getProperty("rust.cargoTargetDir", "CARGO_TARGET_DIR")

        // Determine the NDK version, if present
        val ndkSourceProperties = Properties()
        val ndkSourcePropertiesFile = File(extensions[T::class].ndkDirectory, "source.properties")
        if (ndkSourcePropertiesFile.exists()) {
            ndkSourceProperties.load(ndkSourcePropertiesFile.inputStream())
        }
        val ndkVersion = ndkSourceProperties.getProperty("Pkg.Revision", "0.0")
        val ndkVersionMajor = ndkVersion.split(".").first().toInt()

        // Determine whether to use prebuilt or generated toolchains
        val usePrebuilt =
            config.localProperties.getProperty("rust.prebuiltToolchains")?.equals("true") ?:
            config.prebuiltToolchains ?:
            (ndkVersionMajor >= 19)

        if (usePrebuilt && ndkVersionMajor < 19) {
            throw GradleException("usePrebuilt = true requires NDK version 19+")
        }

        val generateToolchainsTask = "generateToolchains${capitalisedVariantName}"
        val ndkDir = extensions[T::class].ndkDirectory.absolutePath

        tasks.register(generateToolchainsTask, GenerateToolchainsTask::class.java, config, ndkDir).configure {
            it.group = RUST_TASK_GROUP
            it.description = "Generate standard toolchain for given architectures"
        }

        val destDir = "${buildDir}/rustJniLibs/${variant.name}"
        extensions[T::class].apply {
            sourceSets.getByName(variant.name).jniLibs.srcDir(File(destDir, "android"))
            sourceSets.getByName("test${capitalisedVariantName}").resources.srcDir(File(destDir, "desktop"))
        }

        val variantAndroidBuildTask = "cargoBuildAndroid${capitalisedVariantName}"
        val variantDesktopBuildTask = "cargoBuildDesktop${capitalisedVariantName}"

        tasks.register(variantAndroidBuildTask) {
            it.group = RUST_TASK_GROUP
            it.description = "Build Rust code for Android (${variant.name})"
            it.extensions.add("outDir", "${destDir}/android")
        }
        tasks.named(androidBuildTask).configure {
            it.dependsOn(variantAndroidBuildTask)
        }

        tasks.register(variantDesktopBuildTask) {
            it.group = RUST_TASK_GROUP
            it.description = "Build Rust code for desktop (${variant.name})"
            it.extensions.add("outDir", "${destDir}/desktop")
        }
        tasks.named(hostBuildTask).configure {
            it.dependsOn(variantDesktopBuildTask)
        }

        config.targets!!.forEach { target ->
            val toolchain = toolchains
                .filter {
                    if (usePrebuilt) {
                        it.type != ToolchainType.ANDROID_GENERATED
                    } else {
                        it.type != ToolchainType.ANDROID_PREBUILT
                    }
                }
                .find { it.platform == target }
                ?: throw GradleException("Target $target is not recognized (recognized targets: ${toolchains.map { it.platform }.sorted()}).  Check `local.properties` and `build.gradle`.")

            val buildTask = "cargoBuild${capitalisedVariantName}${target.replaceFirstChar { it.uppercase() }}"

            val destinationDir = when (toolchain.type) {
                ToolchainType.ANDROID_GENERATED -> "android"
                ToolchainType.ANDROID_PREBUILT -> "android"
                ToolchainType.DESKTOP -> "desktop"
            } .let { "${destDir}/${it}/${toolchain.folder}" }

            tasks.register(buildTask, CargoBuildTask::class.java, toolchain, config).configure {
                it.group = RUST_TASK_GROUP
                it.description = "Build library for variant: $capitalisedVariantName ($target)"
                it.manifestDir = fileTree(config.module!!).apply {
                    // Exclude cargo build directories
                    exclude("target/**")
                }
                it.destinationDir = file(destinationDir)

                if (!usePrebuilt) {
                    it.dependsOn(generateToolchainsTask)
                }
                it.dependsOn(generateLinkerWrapperTask)
                it.outputs.upToDateWhen { false }
            }

            if (toolchain.type == ToolchainType.DESKTOP) {
                tasks.named(variantDesktopBuildTask).configure {
                    it.dependsOn(buildTask)
                    it.inputs.dir(destinationDir)
                }
            } else {
                tasks.named(variantAndroidBuildTask).configure {
                    it.dependsOn(buildTask)
                    it.inputs.dir(destinationDir)
                }
            }
        }
    }
}
