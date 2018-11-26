package com.nishtahir

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecSpec
import java.io.File
import java.util.*

sealed class Features {
    class All() : Features()

    data class DefaultAnd(val featureSet: Set<String>) : Features()

    data class NoDefaultBut(val featureSet: Set<String>) : Features()
}

data class FeatureSpec(var features: Features? = null) {
    fun all() {
        this.features = Features.All()
    }

    fun defaultAnd(featureSet: Array<String>) {
        this.features = Features.DefaultAnd(featureSet.toSet())
    }

    fun noDefaultBut(featureSet: Array<String>) {
        this.features = Features.NoDefaultBut(featureSet.toSet())
    }
}

// `CargoExtension` is documented in README.md.
open class CargoExtension {
    lateinit var localProperties: Properties

    var module: String? = null
    var libname: String? = null
    var targets: List<String>? = null
    var profile: String = "debug"
    var verbose: Boolean? = null
    var targetDirectory: String? = null
    var targetIncludes: Array<String>? = null
    var apiLevel: Int? = null

    // It would be nice to use a receiver here, but there are problems interoperating with Groovy
    // and Kotlin that are just not worth working out.  Another JVM language, yet another dynamic
    // invoke solution :(
    var exec: ((ExecSpec, Toolchain) -> Unit)? = null

    var featureSpec: FeatureSpec = FeatureSpec()

    fun features(action: Action<FeatureSpec>) {
        action.execute(featureSpec)
    }

    val toolchainDirectory: File
        get() {
            // Share a single toolchain directory, if one is configured.  Prefer "local.properties"
            // to "ANDROID_NDK_TOOLCHAIN_DIR" to "$TMP/rust-android-ndk-toolchains".
            val local: String? = localProperties.getProperty("rust.androidNdkToolchainDir")
            if (local != null) {
                return File(local).absoluteFile
            }

            val globalDir: String? = System.getenv("ANDROID_NDK_TOOLCHAIN_DIR")
            if (globalDir != null) {
                return File(globalDir).absoluteFile
            }

            var defaultDir = File(System.getProperty("java.io.tmpdir"), "rust-android-ndk-toolchains")
            return defaultDir.absoluteFile
        }

    val cargoCommand: String
        get() {
            return getProperty("rust.cargoCommand", "RUST_ANDROID_GRADLE_CARGO_COMMAND") ?: "cargo"
        }

    val pythonCommand: String
        get() {
            return getProperty("rust.pythonCommand", "RUST_ANDROID_GRADLE_PYTHON_COMMAND") ?: "python"
        }

    internal fun getProperty(camelCaseName: String, snakeCaseName: String): String? {
        val local: String? = localProperties.getProperty(camelCaseName)
        if (local != null) {
            return local
        }
        val global: String? = System.getenv(snakeCaseName)
        if (global != null) {
            return global
        }
        return null
    }
}
