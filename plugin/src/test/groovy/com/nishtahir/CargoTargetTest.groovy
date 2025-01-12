package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class CargoTargetTest extends AbstractTest {
    @Unroll
    def "cargoBuild produces #location for target #target"() {
        given:
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withKotlinDisabled()
        // TODO: .withCargo(...)
                .build()
                .writeProject()

        SimpleCargoProject.builder(temporaryFolder.root)
                .withTargets([target])
                .build()
                .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('cargoBuild', '--info', '--stacktrace')
        // .withDebug(true)
                .build()

        // To ease debugging.
        temporaryFolder.root.eachFileRecurse {
            println(it)
        }

        then:
        buildResult.task(':app:cargoBuild').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuild').outcome == TaskOutcome.SUCCESS
        new File(temporaryFolder.root, "app/build/rustJniLibs/${location}").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/${location}").exists()

        where:
        [target, location] << [
                // Sadly, cross-compiling to macOS targets fails at this time: see
                // https://github.com/rust-lang/rust/issues/84984.
                // ["darwin", "desktop/darwin/librust.dylib"],
                // And so does cross-compiling from macOS to Linux targets.
                // ["linux-x86-64", "desktop/linux-x86-64/librust.so"],
                ["arm64",  "android/arm64-v8a/librust.so"],
                ["x86_64", "android/x86_64/librust.so"],
        ]
    }
}
