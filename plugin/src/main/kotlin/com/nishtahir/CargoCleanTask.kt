package com.nishtahir;

import com.android.build.gradle.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.ByteArrayOutputStream
import java.io.File

open class CargoCleanTask : DefaultTask() {
  
    @Suppress("unused")
    @TaskAction
    fun clean() = with(project) {
        extensions[CargoExtension::class].apply {
            val theCommandLine = mutableListOf(this.cargoCommand)
            theCommandLine.add("clean")
            project.exec { spec ->
                val cargoExtension = this 
                with(spec){
                    val module = File(cargoExtension.module!!)
                    if (module.isAbsolute) {
                        workingDir = module
                    } else {
                        workingDir = File(project.project.projectDir, module.path)
                    }

                    workingDir = workingDir.canonicalFile
                    standardOutput = System.out
                    commandLine = theCommandLine
                }
            }
        }
    }
}