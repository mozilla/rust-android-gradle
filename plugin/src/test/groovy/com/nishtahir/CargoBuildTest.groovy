package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

@MultiVersionTest
class CargoBuildTest extends AbstractTest {
    @Unroll
    def "cargoBuild is invoked with #gradleVersion and Android plugin #androidVersion"() {
        given:
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withKotlinDisabled()
                // TODO: .withCargo(...)
                .build()
                .writeProject()

        SimpleCargoProject.builder(temporaryFolder.root)
            .withTargets(["x86_64"])
            .build()
            .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('cargoBuildAndroid', '--info', '--stacktrace', '--rerun-tasks')
                // .withDebug(true)
                .build()

        // To ease debugging.
        temporaryFolder.root.eachFileRecurse {
            println(it)
        }

        then:
        buildResult.task(':app:cargoBuildAndroid').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuildAndroid').outcome == TaskOutcome.SUCCESS
        new File(temporaryFolder.root, "app/build/rustJniLibs/debug/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "app/build/rustJniLibs/release/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/debug/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/release/android/x86_64/librust.so").exists()

        where:
        [androidVersion, gradleVersion] << TestVersions.allCandidateTestVersions.entries().collect { [it.key, it.value] }
    }
}
