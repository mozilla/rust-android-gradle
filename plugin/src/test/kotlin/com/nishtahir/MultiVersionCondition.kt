package com.nishtahir

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

class MultiVersionCondition : EnabledCondition {
    override fun enabled(kclass: KClass<out Spec>) =
        !System.getProperty("org.gradle.android.testVersion").isNullOrEmpty()
}
