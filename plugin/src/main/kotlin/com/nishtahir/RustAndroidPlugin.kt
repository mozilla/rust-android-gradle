package com.nishtahir

import com.android.build.gradle.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File

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
    fun cc(apiLevel: Int): String = "$platform-$apiLevel/$cc"
    fun ar(apiLevel: Int): String = "$platform-$apiLevel/$ar"
}

@Suppress("unused")
open class RustAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {

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
    }

    private inline fun <reified T : BaseExtension> configurePlugin(project: Project) = with(project) {
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
                CargoBuildTask::class.java).apply {
            group = RUST_TASK_GROUP
            description = "Build library"
        }

        buildTask.dependsOn(generateToolchain)
    }
}
