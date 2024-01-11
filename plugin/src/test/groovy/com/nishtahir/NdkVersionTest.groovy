package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

import com.nishtahir.Versions

class NdkVersionTest extends AbstractTest {
    @Unroll
    def "cargoBuild works with Android NDK version #ndkVersion"() {
        given:
        def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()
        def target = "x86_64"
        def location = "android/x86_64/librust.so"
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

        // To ease debugging.
        temporaryFolder.root.eachFileRecurse {
            System.err.println("before> ${it}")
            if (it.path.endsWith(".gradle") || it.path.endsWith(".properties")) {
                System.err.println(it.text)
            }
        }

        when:
        BuildResult buildResult = withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
                .withProjectDir(temporaryFolder.root)
                .withArguments('cargoBuild', '--info', '--stacktrace')
        // .withDebug(true)
                .build()

        // To ease debugging.
        temporaryFolder.root.eachFileRecurse {
            System.err.println("after> ${it}")
        }

        then:
        buildResult.task(':app:cargoBuild').outcome == TaskOutcome.SUCCESS
        buildResult.task(':library:cargoBuild').outcome == TaskOutcome.SUCCESS
        new File(temporaryFolder.root, "app/build/rustJniLibs/${location}").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/${location}").exists()

        where:
        ndkVersion << [
            // Old LTS NDKs need to be installed manually
            "21.4.7075529",
            "23.1.7779620",
            // NDK versions supported by Github Actions, per
            // https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2004-Readme.md#android
            "24.0.8215888",
            "25.2.9519653",
            "26.1.10909125",
        ]
    }
}
