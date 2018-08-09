package com.nishtahir

import org.gradle.process.ExecSpec

open class CargoExtension {
    var module: String = ""
    var targets: List<String> = emptyList()

    /**
     * The Android NDK API level to target.  Defaults to the minimum SDK version of the Android
     * project's default configuration.
     */
    var apiLevel: Int? = null

    /**
     * The Cargo [release profile](https://doc.rust-lang.org/book/second-edition/ch14-01-release-profiles.html#customizing-builds-with-release-profiles) to build.
     *
     * Defaults to `"debug"`.
     */
    var profile: String = "debug"

    /**
     * The target directory into Cargo which writes built outputs.
     *
     * Defaults to `${module}/target`.
     */
    var targetDirectory: String? = null

    /**
     * Which Cargo built outputs to consider JNI libraries.
     *
     * Defaults to `["*.so", "*.dylib", "*.dll"]`.
     */
    var targetIncludes: Array<String> = arrayOf("*.so", "*.dylib", "*.dll")

    /**
     * Android toolchains know where to put their outputs; it's a well-known value like
     * `armeabi-v7a` or `x86`.  The default toolchain outputs don't know where to put their output;
     * use this to say where.
     *
     * Defaults to `""`.
     */
    var defaultToolchainBuildPrefixDir: String? = ""

    // It would be nice to use a receiver here, but there are problems interoperating with Groovy
    // and Kotlin that are just not worth working out.  Another JVM language, yet another dynamic
    // invoke solution :(
    var exec: ((ExecSpec, Toolchain) -> Unit)? = null
}
