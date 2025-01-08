import groovy.json.JsonBuilder
import java.io.FileInputStream
import java.util.Properties

plugins {
    groovy
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.gradle.test.retry)
}

gradlePlugin {
    website = "https://github.com/mozilla/rust-android-gradle"
    vcsUrl = "https://github.com/mozilla/rust-android-gradle.git"
    plugins {
        create("rustAndroidGradlePlugin") {
            id = "org.mozilla.rust-android-gradle.rust-android"
            implementationClass = "com.nishtahir.RustAndroidPlugin"
            displayName = "Plugin for building Rust with Cargo in Android projects"
            description = "A plugin that helps build Rust JNI libraries with Cargo for use in Android projects."
            tags = listOf("rust", "cargo", "android")
        }
    }
}

val versionProperties = Properties().apply {
    load(FileInputStream("${rootProject.projectDir}/version.properties"))
}

group = "org.mozilla.rust-android-gradle"
version = versionProperties["version"]!!

val isCI = (System.getenv("CI") ?: "false").toBoolean()

// Maps supported Android plugin versions to the versions of Gradle that support it
val supportedVersions = mapOf(
    "8.7.3" to listOf("8.9.0", "8.12.0"),
    "8.6.1" to listOf("8.7.0"),
    "8.1.4" to listOf("8.0.0", "7.6.4"),
    "7.2.0" to listOf("7.3.3", "7.6.4"),
    "7.0.0" to listOf("7.1.1"),
    "4.2.2" to listOf("6.8.3", "7.1.1"),
)

val localRepo = file("${layout.buildDirectory}/local-repo")
publishing {
    repositories {
        maven {
            url = localRepo.toURI()
        }
    }
}

dependencies {
    implementation(gradleApi())
    compileOnly(libs.android.gradlePlugin)

    testImplementation(gradleTestKit())
    testImplementation(libs.android.gradlePlugin)

    testImplementation(platform(libs.spock.bom))
    testImplementation(libs.spock.core) { exclude(group = "org.codehaus.groovy") }
    testImplementation(libs.spock.junit4) { exclude(group = "org.codehaus.groovy") }
    testImplementation(libs.junit.jupiter.api)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)
}

val generatedResources = "${layout.buildDirectory.get()}/generated-resources/main"
tasks {
    register("generateVersions") {
        val outputFile = file("$generatedResources/versions.json")
        inputs.properties["version"] = version
        inputs.properties["supportedVersions"] = supportedVersions
        outputs.dir(generatedResources)
        doLast {
            outputFile.writeText(JsonBuilder(mapOf(
                "version" to version,
                "supportedVersions" to supportedVersions
            )).toPrettyString())
        }
    }
    val generatedBuildResources = "${layout.buildDirectory.get()}/build-resources"
    register("generateTestTasksJson") {
        val outputFile = file("$generatedBuildResources/androidTestTasks.json")
        inputs.properties["supportedVersions"] = supportedVersions
        outputs.dir(generatedBuildResources)
        doLast {
            outputFile.writeText(JsonBuilder(
                supportedVersions.keys.map { androidTestTaskName(it) }.toList()
            ).toString())
        }
    }

    withType<Test>().configureEach {
        dependsOn(publish)
        systemProperties["local.repo"] = localRepo.toURI()
        useJUnitPlatform()
        retry {
            maxRetries = if (isCI) { 1 } else { 0 }
            maxFailures = 20
        }
    }

    supportedVersions.keys.forEach { androidVersion ->
        val testTaskName = androidTestTaskName(androidVersion)
        val jdkVersion = jdkVersionFor(androidVersion)
        val versionSpecificTest = register<Test>(testTaskName) {
            description = "Runs the multi-version tests for AGP $androidVersion (JDK version $jdkVersion)"
            group = "verification"

            javaToolchains {
                javaLauncher = launcherFor {
                    languageVersion = jdkVersion
                }
            }

            systemProperties["org.gradle.android.testVersion"] = androidVersion
        }

        named("check").configure {
            dependsOn(versionSpecificTest)
        }
    }
}

sourceSets {
    main {
        output.dir(
            mapOf("builtBy" to tasks.named("generateVersions")),
            generatedResources
        )
    }
}

fun androidTestTaskName(androidVersion: String) = "testAndroid${normalizeVersion(androidVersion)}"
fun normalizeVersion(version: String) = version.replace("[.\\-]".toRegex(), "_")

fun jdkVersionFor(agpVersion: String) = JavaLanguageVersion.of(
    if (agpVersion.split('.')[0].toInt() >= 8) {
        17
    } else if (agpVersion.split('.')[0].toInt() >= 7) {
        11
    } else {
        8
    }
)