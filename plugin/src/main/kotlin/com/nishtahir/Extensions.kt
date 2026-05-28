package com.nishtahir

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import kotlin.reflect.KClass

const val ANDROID_APPLICATION_PLUGIN_ID = "com.android.application"
const val ANDROID_LIBRARY_PLUGIN_ID = "com.android.library"

operator fun <T : Any> ExtensionContainer.get(type: KClass<T>): T = getByType(type.java)

inline fun Project.configureForAndroidPlugin(
	crossinline application: Project.() -> Unit,
	crossinline library: Project.() -> Unit,
) {
	pluginManager.withPlugin(ANDROID_APPLICATION_PLUGIN_ID) {
		application()
	}
	pluginManager.withPlugin(ANDROID_LIBRARY_PLUGIN_ID) {
		library()
	}
}

inline fun <T> Project.withAndroidExtension(
	application: (AppExtension) -> T,
	library: (LibraryExtension) -> T,
): T {
	return when {
		pluginManager.hasPlugin(ANDROID_APPLICATION_PLUGIN_ID) -> application(extensions[AppExtension::class])
		pluginManager.hasPlugin(ANDROID_LIBRARY_PLUGIN_ID) -> library(extensions[LibraryExtension::class])
		else -> throw GradleException("Android application or library plugin is required")
	}
}
