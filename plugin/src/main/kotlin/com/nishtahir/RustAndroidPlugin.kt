package com.nishtahir

import com.android.build.gradle.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

val RUST_TASK_GROUP = "rust"

val toolchains = listOf(
        Toolchain("arm",
                "arm-linux-androideabi",
                "bin/arm-linux-androideabi-clang",
                "armeabi"),
        Toolchain("mips",
                "mipsel-linux-android",
                "bin/mipsel-linux-android-clang",
                "mips"),
        Toolchain("x86",
                "i686-linux-android",
                "bin/i686-linux-android-clang",
                "x86")
)

data class Toolchain(val platform: String,
                     val target: String,
                     val compiler: String,
                     val folder: String) {
    fun bin(): String = "$platform/$compiler"
}

@Suppress("unused")
open class RustAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        extensions.add("cargo", CargoExtension::class.java)
        afterEvaluate {
            plugins.all {
                when (it) {
                    is AppPlugin -> configurePlugin<AppExtension>(this)
                    is LibraryPlugin -> configurePlugin<LibraryExtension>(this)
                }
            }
        }
    }

    private inline fun <reified T : BaseExtension> configurePlugin(project: Project) = with(project) {
        extensions[T::class].apply {
            sourceSets.getByName("main").jniLibs.srcDir(File("$buildDir/jniLibs/"))
        }

        val generateToolchain = tasks.maybeCreate("generateToolchains",
                GenerateToolchainsTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Generate standard toolchain for given architectures"
        }

        val buildTask = tasks.maybeCreate("cargoBuild",
                CargoBuildTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library"
        }

        buildTask.dependsOn(generateToolchain)
    }
}

