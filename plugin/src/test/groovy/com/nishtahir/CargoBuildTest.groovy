package com.nishtahir

import org.gradle.api.GradleException
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import com.nishtahir.Versions


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
                .withArguments('mergeDebugJniLibFolders', '--info', '--stacktrace')
                // .withDebug(true)
                .build()

        // To ease debugging.
        temporaryFolder.root.eachFileRecurse {
            println(it)
        }

        then:
        buildResult.task(':app:cargoBuild').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuild').outcome == TaskOutcome.SUCCESS
        buildResult.task(':app:cargoBuildX86_64').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuildX86_64').outcome == TaskOutcome.SUCCESS
        new File(temporaryFolder.root, "app/build/rustJniLibs/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "app/build/intermediates/merged_jni_libs/debug/mergeDebugJniLibFolders/out/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "library/build/intermediates/merged_jni_libs/debug/mergeDebugJniLibFolders/out/x86_64/librust.so").exists()

        where:
        [androidVersion, gradleVersion] << TestVersions.allCandidateTestVersions.entries().collect { [it.key, it.value] }
    }
}
