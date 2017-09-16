package com.nishtahir

import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import kotlin.reflect.KClass

val TOOLS_FOLDER = ".cargo/toolchain"
val TARGET_FOLDER = "build/"

operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T {
    return getByType(type.java)!!
}

fun Project.getToolchainDirectory(): File {
    return File(projectDir, TOOLS_FOLDER)
}