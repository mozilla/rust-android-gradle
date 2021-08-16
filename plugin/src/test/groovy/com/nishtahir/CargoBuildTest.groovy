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

        def cargoModule = this.class.classLoader.getResource("rust/Cargo.toml").path
        cargoModule = new File(cargoModule).parent

        file('app/build.gradle') << """
            cargo {
                module = "${cargoModule}"
                targetDirectory = "${cargoModule}/../target"
                targets = ["x86_64"]
                libname = "rust"
            }
        """.stripIndent()

        file('library/build.gradle') << """
            cargo {
                module = "${cargoModule}"
                targetDirectory = "${cargoModule}/../target"
                targets = ["x86_64"]
                libname = "rust"
            }
        """.stripIndent()

        when:
        BuildResult buildResult = withGradleVersion(gradleVersion.version)
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
        new File(temporaryFolder.root, "app/build/rustJniLibs/android/x86_64/librust.so").exists()
        new File(temporaryFolder.root, "library/build/rustJniLibs/android/x86_64/librust.so").exists()

        where:
        [androidVersion, gradleVersion] << TestVersions.allCandidateTestVersions.entries().collect { [it.key, it.value] }
    }
}
