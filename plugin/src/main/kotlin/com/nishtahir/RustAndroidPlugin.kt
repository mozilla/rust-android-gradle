package com.nishtahir

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasUnitTest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import java.io.File
import java.util.Properties

const val RUST_TASK_GROUP = "rust"

enum class ToolchainType {
    ANDROID_PREBUILT,
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

data class Ndk(val path: File, val version: String) : java.io.Serializable {
    val versionMajor: Int
        get() = version.split(".").first().toInt()
}

data class Toolchain(val platform: String,
                     val type: ToolchainType,
                     val target: String,
                     val compilerTriple: String,
                     val binutilsTriple: String,
                     val folder: String) : java.io.Serializable {
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

    override fun apply(project: Project) {
        with(project) {
            cargoExtension = extensions.create("cargo", CargoExtension::class.java)

            listOf("com.android.application", "com.android.library").forEach { id ->
                pluginManager.withPlugin(id) {
                    configureAndroid(this)
                }
            }
        }
    }

    private fun configureAndroid(project: Project) = with(project) {
        val androidComponents = extensions.getByType(AndroidComponentsExtension::class.java)
        var minSdk: Int? = null

        androidComponents.onVariants { variant ->
            val rustJniLibs = layout.buildDirectory.dir("rustJniLibs").get().asFile
            minSdk = minOf(minSdk ?: Int.MAX_VALUE, variant.minSdk.apiLevel)
            variant.sources.jniLibs?.addStaticSourceDirectory(File(rustJniLibs, "android").path)
            val mergeJniLibs = "merge${variant.name.replaceFirstChar { it.titlecase() }}JniLibFolders"
            tasks.named { it == mergeJniLibs }.configureEach { it.dependsOn("cargoBuild") }
            (variant as? HasUnitTest)?.unitTest?.configureTestTask {
                it.classpath += files(File(rustJniLibs, "desktop"))
                it.dependsOn("cargoBuild")
            }
        }

        afterEvaluate {
            configurePlugin(this, androidComponents, minSdk)
        }
    }

    private fun configurePlugin(project: Project, androidComponents: AndroidComponentsExtension<*, *, *>, minSdk: Int?) = with(project) {
        cargoExtension.localProperties = Properties()

        val localPropertiesFile = File(project.rootDir, "local.properties")
        if (localPropertiesFile.exists()) {
            cargoExtension.localProperties.load(localPropertiesFile.inputStream())
        }

        if (cargoExtension.module == null) {
            throw GradleException("module cannot be null")
        }

        if (cargoExtension.libname == null) {
            throw GradleException("libname cannot be null")
        }

        // Allow to set targets, including per-project, in local.properties.
        val localTargets: String? =
                cargoExtension.localProperties.getProperty("rust.targets.${project.name}") ?:
                cargoExtension.localProperties.getProperty("rust.targets")
        if (localTargets != null) {
            cargoExtension.targets = localTargets.split(',').map { it.trim() }
        }

        if (cargoExtension.targets == null) {
            throw GradleException("targets cannot be null")
        }

        // Ensure that an API level is specified for all targets
        val apiLevel = cargoExtension.apiLevel
        if (cargoExtension.apiLevels.size > 0) {
            if (apiLevel != null) {
                throw GradleException("Cannot set both `apiLevel` and `apiLevels`")
            }
        } else {
            val default = apiLevel
                ?: minSdk
                ?: throw GradleException("Could not determine minSdk: no Android variants were configured")
            cargoExtension.apiLevels = cargoExtension.targets!!.map { it to default }.toMap()
        }
        val missingApiLevelTargets = cargoExtension.targets!!.toSet().minus(
            cargoExtension.apiLevels.keys)
        if (missingApiLevelTargets.size > 0) {
            throw GradleException("`apiLevels` missing entries for: $missingApiLevelTargets")
        }

        // Determine the NDK version, if present
        val ndk = androidComponents.sdkComponents.ndkDirectory.get().asFile.let {
            val ndkSourceProperties = Properties()
            val ndkSourcePropertiesFile = File(it, "source.properties")
            if (ndkSourcePropertiesFile.exists()) {
                ndkSourceProperties.load(ndkSourcePropertiesFile.inputStream())
            }
            val ndkVersion = ndkSourceProperties.getProperty("Pkg.Revision", "0.0")
            Ndk(path = it, version = ndkVersion)
        }

        // Fish linker wrapper scripts from our Java resources.
        val generateLinkerWrapper = rootProject.tasks.maybeCreate("generateLinkerWrapper", Sync::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Generate shared linker wrapper script"
        }

        generateLinkerWrapper.apply {
            // From https://stackoverflow.com/a/320595.
            from(rootProject.zipTree(File(RustAndroidPlugin::class.java.protectionDomain.codeSource.location.toURI()).path))
            include("**/linker-wrapper*")
            into(rootProject.layout.buildDirectory.dir("linker-wrapper"))
            eachFile {
                it.path = it.path.replaceFirst("com/nishtahir", "")
            }
            filePermissions { permissions ->
                permissions.unix("rwxr-xr-x") // 0755
            }
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        val buildTask = tasks.maybeCreate("cargoBuild",
                DefaultTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library (all targets)"
        }

        cargoExtension.targets!!.forEach { target ->
            val theToolchain = toolchains
                    .find { it.platform == target }
            if (theToolchain == null) {
                throw GradleException("Target ${target} is not recognized (recognized targets: ${toolchains.map { it.platform }.sorted()}).  Check `local.properties` and `build.gradle`.")
            }

            val targetBuildTask = tasks.maybeCreate("cargoBuild${target.replaceFirstChar { it.titlecase() }}",
                    CargoBuildTask::class.java).apply {
                group = RUST_TASK_GROUP
                description = "Build library ($target)"
                toolchain = theToolchain
                this.ndk = ndk
                projectDir = project.projectDir
                this.buildDir = project.layout.buildDirectory.get().asFile
                rootBuildDir = project.rootProject.layout.buildDirectory.get().asFile
                cargoCommand = cargoExtension.cargoCommand
                rustcCommand = cargoExtension.rustcCommand
                rustupChannel = cargoExtension.rustupChannel
                pythonCommand = cargoExtension.pythonCommand
                module = cargoExtension.module!!
                libname = cargoExtension.libname
                verbose = cargoExtension.verbose
                profile = cargoExtension.profile
                cargoTargetDir = cargoExtension.getProperty("rust.cargoTargetDir", "CARGO_TARGET_DIR")
                    ?: cargoExtension.targetDirectory
                targetIncludes = cargoExtension.targetIncludes
                featureSpec = cargoExtension.featureSpec
                extraCargoBuildArguments = cargoExtension.extraCargoBuildArguments
                apiLevels = cargoExtension.apiLevels
                generateBuildId = cargoExtension.generateBuildId
                this.toolchainDirectory = cargoExtension.toolchainDirectory
                autoConfigureClangSys = cargoExtension.getFlagProperty(
                    "rust.autoConfigureClangSys",
                    "RUST_ANDROID_GRADLE_AUTO_CONFIGURE_CLANG_SYS",
                    theToolchain.type != ToolchainType.DESKTOP
                )
                targetProperties = project.extensions.extraProperties.properties
                    .filterKeys { it.startsWith("RUST_ANDROID_GRADLE_TARGET_") }
                    .mapValues { it.value?.toString() ?: "" }
                cargoExtension.exec?.let {
                    logger.warn("rust-android-gradle: cargo.exec closure is not compatible with Gradle configuration cache")
                    execClosure = it
                }
            }

            targetBuildTask.dependsOn(generateLinkerWrapper)
            buildTask.dependsOn(targetBuildTask)
        }
    }
}
