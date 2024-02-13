package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class CargoTargetTest extends AbstractTest {
    @Unroll
    def "cargoBuild produces #location for target #target"() {
        given:
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def ndkVersion = "21.4.7075529"
        def ndkVersionMajor = ndkVersion.split('\\.')[0] as int
        // Toolchain 1.68 or later versions are not compatible to old NDK prior to r23
        // https://blog.rust-lang.org/2023/01/09/android-ndk-update-r25.html
        def channel = ndkVersionMajor >= 23 ? "stable" : "1.67"

        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withNdkVersion(ndkVersion)
                .withKotlinDisabled()
        // TODO: .withCargo(...)
                .build()
                .writeProject()

        SimpleCargoProject.builder(temporaryFolder.root)
                .withTargets([target])
                .withChannel(channel)
                .build()
                .writeProject()

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('cargoBuildAndroid', '--stacktrace', '--rerun-tasks')
        // .withDebug(true)
                .build()

        // To ease debugging.
//        temporaryFolder.root.eachFileRecurse {
//            println(it)
//        }

        then:
        buildResult.task(':app:cargoBuildAndroid').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuildAndroid').outcome == TaskOutcome.SUCCESS
        file("app/build/rustJniLibs/debug/${location}").exists()
        file("app/build/rustJniLibs/release/${location}").exists()
        file("library/build/rustJniLibs/debug/${location}").exists()
        file("library/build/rustJniLibs/release/${location}").exists()

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
