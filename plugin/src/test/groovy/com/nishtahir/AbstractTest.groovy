package com.nishtahir

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class AbstractTest extends Specification {
    @Rule TemporaryFolder temporaryFolder
    File cacheDir

    def setup() {
        cacheDir = temporaryFolder.newFolder()
    }

    def withGradleVersion(String gradleVersion) {
        if (gradleVersion.count(".") == 2 && gradleVersion.endsWith(".0")) {
            gradleVersion = gradleVersion.substring(0, gradleVersion.length() - 2)
        }
        GradleRunner.create()
            .withGradleVersion(gradleVersion)
            .forwardOutput()
            .withDebug(false)
    }

    File file(String path) {
        return new File(temporaryFolder.root, path)
    }
}
