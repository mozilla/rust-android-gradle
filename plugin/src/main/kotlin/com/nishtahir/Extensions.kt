package com.nishtahir

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import java.util.Properties
import kotlin.reflect.KClass

operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T = getByType(type.java)

fun Project.getToolchainDirectory(): File {
    // Share a single toolchain directory, if one is configured.  Prefer "local.properties"
    // to "ANDROID_NDK_TOOLCHAIN_DIR" to "$TMP/rust-android-ndk-toolchains".
    val localPropertiesFile = File(rootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        val localProperties = Properties()
        localProperties.load(localPropertiesFile.inputStream())
        if (localProperties.containsKey("rust.androidNdkToolchainDir")) {
            return File(localProperties.getProperty("rust.androidNdkToolchainDir")).absoluteFile
        }
    }

    val globalDir: String? = System.getenv("ANDROID_NDK_TOOLCHAIN_DIR")
    if (globalDir != null) {
        return File(globalDir).absoluteFile
    }

    var defaultDir = File(System.getProperty("java.io.tmpdir"), "rust-android-ndk-toolchains")
    return defaultDir.absoluteFile
}
