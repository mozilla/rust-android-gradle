package com.nishtahir

import org.gradle.testkit.runner.BuildResult
import spock.lang.IgnoreIf

@MultiVersionTest
class CargoEnvironmentTest extends AbstractTest {
    static final String PREFIX = "RUST_ANDROID_GRADLE_TARGET_X86_64_LINUX_ANDROID_"

    def androidVersion = TestVersions.latestAndroidVersionForCurrentJDK()

    private void writeProject() {
        SimpleAndroidApp.builder(temporaryFolder.root, cacheDir)
                .withAndroidVersion(androidVersion)
                .withKotlinDisabled()
                .build()
                .writeProject()

        SimpleCargoProject.builder(temporaryFolder.root)
                .withTargets(["x86_64"])
                .withChannel("stable")
                .build()
                .writeProject()
    }

    private BuildResult build(Map<String, String> environment = [:], String... arguments) {
        return withGradleVersion(TestVersions.latestSupportedGradleVersionFor(androidVersion).version)
                .withProjectDir(temporaryFolder.root)
                .withEnvironment(System.getenv() + environment)
                .withArguments(arguments.toList() + "--stacktrace")
                .build()
    }

    private List<String> cargoBuildEnvironment(Map<String, String> environment = [:], String... arguments) {
        def dump = file("cargo-env.txt")
        def wrapper = file("rustc-wrapper.sh")
        wrapper.text = "#!/bin/sh\nenv >> '${dump.path}'\nexec \"\$@\"\n"
        wrapper.setExecutable(true)

        build(environment, *([":app:cargoBuild", "-P${PREFIX}RUSTC_WRAPPER=${wrapper.path}"] + arguments.toList()))
        return dump.readLines()
    }

    @IgnoreIf({ os.windows })
    def "target properties from every source reach cargo"() {
        given:
        writeProject()
        file("gradle.properties") << "\n${PREFIX}FROM_ROOT_FILE=root\n"
        file("app/gradle.properties") << "${PREFIX}FROM_SUBPROJECT_FILE=sub\n"
        file("app/build.gradle") << "\next.${PREFIX}FROM_EXT = 'ext'\n"

        when:
        def env = cargoBuildEnvironment([("ORG_GRADLE_PROJECT_${PREFIX}FROM_ENV".toString()): "env"], "-P${PREFIX}FROM_CLI=cli")

        then:
        env.contains("FROM_CLI=cli")
        env.contains("FROM_ENV=env")
        env.contains("FROM_ROOT_FILE=root")
        env.contains("FROM_SUBPROJECT_FILE=sub")
        env.contains("FROM_EXT=ext")
    }

    static final String FLAVORS_WITH_LOWER_MIN_SDK = """
        android {
            flavorDimensions += "tier"
            productFlavors {
                high { dimension = "tier" }
                low { dimension = "tier"; minSdk = 26 }
                mid { dimension = "tier"; minSdk = 28 }
            }
        }
    """.stripIndent()

    @IgnoreIf({ os.windows })
    def "cargo builds with API level #expected for #description"() {
        given:
        writeProject()
        file("app/build.gradle") << config

        when:
        def env = cargoBuildEnvironment()

        then:
        env.any { it ==~ /RUST_ANDROID_GRADLE_CC=.*x86_64-linux-android${expected}-clang/ }

        where:
        description                     | config                          | expected
        "the lowest variant minSdk"     | FLAVORS_WITH_LOWER_MIN_SDK      | 26
        "an explicit apiLevel"          | "\ncargo { apiLevel = 24 }\n" | 24
    }

    def "#task depends on cargoBuild"() {
        given:
        writeProject()
        file("app/build.gradle") << config

        when:
        def result = build(task, "--dry-run")

        then:
        result.output.contains(":app:cargoBuild SKIPPED")

        where:
        task                               | config
        ":app:testDebugUnitTest"           | ""
        ":app:mergeLowDebugJniLibFolders"  | FLAVORS_WITH_LOWER_MIN_SDK
        ":app:testLowDebugUnitTest"        | FLAVORS_WITH_LOWER_MIN_SDK
    }

    def "desktop libraries are on the unit test classpath"() {
        given:
        writeProject()
        file("app/build.gradle") << """
            tasks.register("printUnitTestClasspath") {
                doLast { println("UNIT_TEST_CLASSPATH=" + tasks.getByName("testDebugUnitTest").classpath.files) }
            }
        """.stripIndent()

        when:
        def result = build(":app:printUnitTestClasspath")

        then:
        result.output.contains(new File(file("app").canonicalFile, "build/rustJniLibs/desktop").path)
        result.output.contains("compileDebugUnitTestJavaWithJavac")
    }
}
