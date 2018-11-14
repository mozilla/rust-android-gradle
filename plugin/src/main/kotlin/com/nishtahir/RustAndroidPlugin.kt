package com.nishtahir

import com.android.build.gradle.*
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.Properties

const val RUST_TASK_GROUP = "rust"

val toolchains = listOf(
        Toolchain("default",
                null,
                "<cc>",
                "<ar>",
                "default"),
        Toolchain("arm",
                "armv7-linux-androideabi",
                "bin/arm-linux-androideabi-clang",
                "bin/arm-linux-androideabi-ar",
                "armeabi-v7a"),
        Toolchain("arm64",
                "aarch64-linux-android",
                "bin/aarch64-linux-android-clang",
                "bin/aarch64-linux-android-ar",
                "arm64-v8a"),
        Toolchain("x86",
                "i686-linux-android",
                "bin/i686-linux-android-clang",
                "bin/i686-linux-android-ar",
                "x86"),
        Toolchain("x86_64",
                "x86_64-linux-android",
                "bin/x86_64-linux-android-clang",
                "bin/x86_64-linux-android-ar",
                "x86_64")
)

data class Toolchain(val platform: String,
                     val target: String?,
                     val cc: String,
                     val ar: String,
                     val folder: String) {
    fun cc(apiLevel: Int): File =
        if (System.getProperty("os.name").startsWith("Windows")) {
            File("$platform-$apiLevel", "$cc.cmd")
        } else {
            File("$platform-$apiLevel", "$cc")
        }

    fun ar(apiLevel: Int): File = File("$platform-$apiLevel", "$ar")
}

@Suppress("unused")
open class RustAndroidPlugin : Plugin<Project> {
    internal lateinit var cargoExtension: CargoExtension

    override fun apply(project: Project) {
        with(project) {
            cargoExtension = extensions.create("cargo", CargoExtension::class.java)

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

        extensions[T::class].apply {
            sourceSets.getByName("main").jniLibs.srcDir(File("$buildDir/rustJniLibs/"))
            sourceSets.getByName("test").resources.srcDir(File("$buildDir/rustResources/"))
        }

        val generateToolchain = tasks.maybeCreate("generateToolchains",
                GenerateToolchainsTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Generate standard toolchain for given architectures"
        }

        val buildTask = tasks.maybeCreate("cargoBuild",
                DefaultTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library (all targets)"
        }

        cargoExtension.targets!!.forEach { target ->
            val targetBuildTask = tasks.maybeCreate("cargoBuild${target.capitalize()}",
                    CargoBuildTask::class.java).apply {
                group = RUST_TASK_GROUP
                description = "Build library ($target)"
                toolchain = toolchains.find { (arch) -> arch == target }
            }

            targetBuildTask.dependsOn(generateToolchain)
            buildTask.dependsOn(targetBuildTask)
        }
    }
}
