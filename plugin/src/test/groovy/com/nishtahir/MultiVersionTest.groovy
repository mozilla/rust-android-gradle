package com.nishtahir

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Represents tests that span multiple versions of Android Gradle Plugin and need to be executed
 * with multiple versions of the JDK.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface MultiVersionTest { }
