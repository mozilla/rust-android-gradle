package com.nishtahir

import org.gradle.api.Action
import org.gradle.process.ExecSpec

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
    var module: String? = null
    var libname: String? = null
    var targets: List<String>? = null
    var profile: String = "debug"
    var verbose: Boolean? = null
    var targetDirectory: String? = null
    var targetIncludes: Array<String>? = null
    var defaultToolchainBuildPrefixDir: String? = ""
    var apiLevel: Int? = null

    // It would be nice to use a receiver here, but there are problems interoperating with Groovy
    // and Kotlin that are just not worth working out.  Another JVM language, yet another dynamic
    // invoke solution :(
    var exec: ((ExecSpec, Toolchain) -> Unit)? = null

    var featureSpec: FeatureSpec = FeatureSpec()

    fun features(action: Action<FeatureSpec>) {
        action.execute(featureSpec)
    }
}
