package com.nishtahir

import org.gradle.api.plugins.ExtensionContainer
import kotlin.reflect.KClass

operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T = getByType(type.java)
