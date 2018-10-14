package com.nishtahir

import org.gradle.process.ExecSpec

// `CargoExtension` is documented in README.md.
open class CargoExtension {
    var module: String? = null
    var libname: String? = null
    var targets: List<String>? = null
    var profile: String = "debug"
    var targetDirectory: String? = null
    var targetIncludes: Array<String>? = null
    var defaultToolchainBuildPrefixDir: String? = ""
    var apiLevel: Int? = null

    // It would be nice to use a receiver here, but there are problems interoperating with Groovy
    // and Kotlin that are just not worth working out.  Another JVM language, yet another dynamic
    // invoke solution :(
    var exec: ((ExecSpec, Toolchain) -> Unit)? = null
}
