package com.nishtahir

import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import java.io.File
import java.util.Properties

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
                "desktop/linux-x86-64"),
        // This should eventually go away: the darwin-x86-64 target will supersede it.
        // https://github.com/mozilla/rust-android-gradle/issues/77
        Toolchain("darwin",
                ToolchainType.DESKTOP,
                "x86_64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "desktop/darwin"),
        Toolchain("darwin-x86-64",
                ToolchainType.DESKTOP,
                "x86_64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "desktop/darwin-x86-64"),
        Toolchain("darwin-aarch64",
                ToolchainType.DESKTOP,
                "aarch64-apple-darwin",
                "<compilerTriple>",
                "<binutilsTriple>",
                "desktop/darwin-aarch64"),
        Toolchain("win32-x86-64-msvc",
                ToolchainType.DESKTOP,
                "x86_64-pc-windows-msvc",
                "<compilerTriple>",
                "<binutilsTriple>",
                "desktop/win32-x86-64"),
        Toolchain("win32-x86-64-gnu",
                ToolchainType.DESKTOP,
                "x86_64-pc-windows-gnu",
                "<compilerTriple>",
                "<binutilsTriple>",
                "desktop/win32-x86-64"),
        Toolchain("arm",
                ToolchainType.ANDROID_GENERATED,
                "armv7-linux-androideabi",
                "arm-linux-androideabi",
                "arm-linux-androideabi",
                "android/armeabi-v7a"),
        Toolchain("arm64",
                ToolchainType.ANDROID_GENERATED,
                "aarch64-linux-android",
                "aarch64-linux-android",
                "aarch64-linux-android",
                "android/arm64-v8a"),
        Toolchain("x86",
                ToolchainType.ANDROID_GENERATED,
                "i686-linux-android",
                "i686-linux-android",
                "i686-linux-android",
                "android/x86"),
        Toolchain("x86_64",
                ToolchainType.ANDROID_GENERATED,
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64-linux-android",
                "android/x86_64"),
        Toolchain("arm",
                ToolchainType.ANDROID_PREBUILT,
                "armv7-linux-androideabi",  // This is correct.  "Note: For 32-bit ARM, the compiler is prefixed with
                "armv7a-linux-androideabi", // armv7a-linux-androideabi, but the binutils tools are prefixed with
                "arm-linux-androideabi",    // arm-linux-androideabi. For other architectures, the prefixes are the same
                "android/armeabi-v7a"),     // for all tools."  (Ref: https://developer.android.com/ndk/guides/other_build_systems#overview )
        Toolchain("arm64",
                ToolchainType.ANDROID_PREBUILT,
                "aarch64-linux-android",
                "aarch64-linux-android",
                "aarch64-linux-android",
                "android/arm64-v8a"),
        Toolchain("x86",
                ToolchainType.ANDROID_PREBUILT,
                "i686-linux-android",
                "i686-linux-android",
                "i686-linux-android",
                "android/x86"),
        Toolchain("x86_64",
                ToolchainType.ANDROID_PREBUILT,
                "x86_64-linux-android",
                "x86_64-linux-android",
                "x86_64-linux-android",
                "android/x86_64")
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
    internal lateinit var cargoExtension: CargoExtension
    internal lateinit var buildWholeTask: DefaultTask

    override fun apply(project: Project) {
        with(project) {
            cargoExtension = extensions.create("cargo", CargoExtension::class.java, this)

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
        buildWholeTask = tasks.maybeCreate("cargoBuild", DefaultTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library (all variants)"
        }

        val androidExtension = extensions[T::class]
        when (androidExtension) {
            is AppExtension -> androidExtension.applicationVariants.all { variant -> 
                configureForVariant<T>(project, variant)
            }
            is LibraryExtension -> androidExtension.libraryVariants.all { variant ->
                configureForVariant<T>(project, variant)
            }
        }

    }

    private inline fun <reified T: BaseExtension> configureForVariant(project: Project, variant: BaseVariant) = with(project) {
        val capitalisedVariantName = variant.name.capitalize()
        var config = cargoExtension.getConfig(variant)

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

        // Allow to set targets, including per-project, in local.properties.
        val localTargets: String? =
                config.localProperties.getProperty("rust.targets.${project.name}") ?:
                config.localProperties.getProperty("rust.targets")
        if (localTargets != null) {
            config.targets = localTargets.split(',').map { it.trim() }
        }

        if (config.targets == null) {
            throw GradleException("targets cannot be null")
        }

        // Ensure that an API level is specified for all targets
        val apiLevel = config.apiLevel
        if (config.apiLevels.size > 0) {
            if (apiLevel != null) {
                throw GradleException("Cannot set both `apiLevel` and `apiLevels`")
            }
        } else {
            val default = if (apiLevel != null) {
                apiLevel
            } else {
                extensions[T::class].defaultConfig.minSdkVersion!!.apiLevel
            }
            config.apiLevels = config.targets!!.map { it to default }.toMap()
        }
        val missingApiLevelTargets = config.targets!!.toSet().minus(
            config.apiLevels.keys)
        if (missingApiLevelTargets.size > 0) {
            throw GradleException("`apiLevels` missing entries for: $missingApiLevelTargets")
        }

        extensions[T::class].apply {
            sourceSets.getByName("main").jniLibs.srcDir(File("$buildDir/rustJniLibs/android"))
            sourceSets.getByName("test").resources.srcDir(File("$buildDir/rustJniLibs/desktop"))
        }

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
            (ndkVersionMajor >= 19);

        if (usePrebuilt && ndkVersionMajor < 19) {
            throw GradleException("usePrebuilt = true requires NDK version 19+")
        }

        val generateToolchain = if (!usePrebuilt) {
            tasks.maybeCreate("generateToolchains",
                    GenerateToolchainsTask::class.java).apply {
                group = RUST_TASK_GROUP
                description = "Generate standard toolchain for given architectures"
            }
        } else {
            null
        }

        // Fish linker wrapper scripts from our Java resources.
        val generateLinkerWrapper = rootProject.tasks.maybeCreate("generateLinkerWrapper", GenerateLinkerWrapperTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Generate shared linker wrapper script"
        }

        generateLinkerWrapper.apply {
            // From https://stackoverflow.com/a/320595.
            from(rootProject.zipTree(File(RustAndroidPlugin::class.java.protectionDomain.codeSource.location.toURI()).path))
            include("**/linker-wrapper*")
            into(File(rootProject.buildDir, "linker-wrapper"))
            eachFile {
                it.path = it.path.replaceFirst("com/nishtahir", "")
            }
            fileMode = 493 // 0755 in decimal; Kotlin doesn't have octal literals (!).
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        val buildTask = tasks.maybeCreate("cargoBuild${capitalisedVariantName}",
                DefaultTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library for variant: ${capitalisedVariantName}"
        }

        buildWholeTask.dependsOn(buildTask)

        config.targets!!.forEach { target ->
            val theToolchain = toolchains
                    .filter {
                        if (usePrebuilt) {
                            it.type != ToolchainType.ANDROID_GENERATED
                        } else {
                            it.type != ToolchainType.ANDROID_PREBUILT
                        }
                    }
                    .find { it.platform == target }
            if (theToolchain == null) {
                throw GradleException("Target ${target} is not recognized (recognized targets: ${toolchains.map { it.platform }.sorted()}).  Check `local.properties` and `build.gradle`.")
            }

            val targetBuildTask = tasks.maybeCreate("cargoBuild${capitalisedVariantName}${target.capitalize()}",
                    CargoBuildTask::class.java).apply {
                group = RUST_TASK_GROUP
                description = "Build library for variant: ${capitalisedVariantName} ($target)"
                toolchain = theToolchain
                cargoConfig = config
            }

            if (!usePrebuilt) {
                targetBuildTask.dependsOn(generateToolchain!!)
            }
            targetBuildTask.dependsOn(generateLinkerWrapper)
            buildTask.dependsOn(targetBuildTask)
        }
    }
}
