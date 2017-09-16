package com.nishtahir

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

val toolchains = listOf<Toolchain>(
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

open class RustAndroidPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        extensions.add("cargo", CargoExtension::class.java)

        afterEvaluate {
            plugins.all {
                when (it) {
                    is AppPlugin -> {
                        val app = extensions[AppExtension::class]
                        val minSdk = app.defaultConfig.minSdkVersion.apiLevel
                        val ndk = app.ndkDirectory
                        println("Checking for existing toolchain in ${getToolchainDirectory().absolutePath}")
                        if (!getToolchainDirectory().exists()) {
                            println("Preparing standalone toolchains...")
                            toolchains.filterNot { (arch) -> minSdk < 21 && arch.endsWith("64") }
                                    .forEach { (arch) ->
                                        exec { spec ->
                                            spec.standardOutput = System.out
                                            spec.commandLine = listOf("$ndk/build/tools/make_standalone_toolchain.py")
                                            spec.args = listOf("--arch=$arch", "--api=$minSdk", "--install-dir=${getToolchainDirectory()}/$arch")
                                        }
                                    }
                        } else {
                            println("Existing toolchains found. Clean to regenerate")
                        }
                        app.sourceSets.getByName("main").jniLibs.srcDir(File("$buildDir/jniLibs/"))
                        tasks.create("cargoBuild", CargoBuildTask::class.java)
                    }
                }
            }
        }
    }
}

