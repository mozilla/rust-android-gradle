package com.nishtahir

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T = getByType(type.java)

inline operator fun<reified T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>): T = this.get()
inline operator fun<reified T> Property<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) = this.set(value)

fun Project.rootBuildDirectory(): Provider<File> = rootProject.layout.buildDirectory.asFile
fun Project.buildDirectory(): Provider<File> = layout.buildDirectory.asFile

inline fun<reified T> DefaultTask.property(): Property<T> = project.objects.property(T::class.java)

fun CharSequence.capitalized() = toString().replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
