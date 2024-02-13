package com.nishtahir

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class AbstractTest extends Specification {
    @Rule TemporaryFolder temporaryFolder
    File cacheDir

    def setup() {
//        cacheDir = temporaryFolder.newFolder()
        cacheDir = new File(System.getProperty("user.home"), ".gradle/caches")
    }

    def withGradleVersion(String gradleVersion) {
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withDebug(false)
    }

    File file(String path) {
        return new File(temporaryFolder.root, path)
    }
}
