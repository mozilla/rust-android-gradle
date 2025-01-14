plugins {
    id("com.android.application") version("8.7.3")
    id("me.sigptr.rust-android")
}

android {
    namespace = "com.nishtahir.androidrust"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.nishtahir.androidrust"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("test") {
            resources {
                srcDir(layout.buildDirectory.dir("rustJniLibs/desktop"))
            }
        }
        getByName("main")
    }
}

cargo {
    module = "../rust"
    targets = listOf("x86_64", "linux-x86-64")
    libname = "rust"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// There's an interaction between Gradle's resolution of dependencies with different types
// (@jar, @aar) for `implementation` and `testImplementation` and with Android Studio's built-in
// JUnit test runner.  The runtime classpath in the built-in JUnit test runner gets the
// dependency from the `implementation`, which is type @aar, and therefore the JNA dependency
// doesn't provide the JNI dispatch libraries in the correct Java resource directories.  I think
// what's happening is that @aar type in `implementation` resolves to the @jar type in
// `testImplementation`, and that it wins the dependency resolution battle.
//
// A workaround is to add a new configuration which depends on the @jar type and to reference
// the underlying JAR file directly in `testImplementation`.  This JAR file doesn't resolve to
// the @aar type in `implementation`.  This works when invoked via `gradle`, but also sets the
// correct runtime classpath when invoked with Android Studio's built-in JUnit test runner.
// Success!
val jnaForTest by configurations.creating

dependencies {
    jnaForTest("net.java.dev.jna:jna:5.6.0@jar")
    implementation("net.java.dev.jna:jna:5.6.0@aar")

    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2") {
        exclude("com.android.support:support-annotations")
    }
    implementation("com.android.support:appcompat-v7:28.0.0")
    implementation("com.android.support.constraint:constraint-layout:2.0.4")
    testImplementation("junit:junit:4.13.2")

    // For reasons unknown, resolving the jnaForTest configuration directly
    // trips a nasty issue with the Android-Gradle plugin 3.2.1, like `Cannot
    // change attributes of configuration ':PROJECT:kapt' after it has been
    // resolved`.  I think that the configuration is being made a
    // super-configuration of the testImplementation and then the `.files` is
    // causing it to be resolved.  Cloning first dissociates the configuration,
    // avoiding other configurations from being resolved.  Tricky!
    testImplementation(files(jnaForTest.copyRecursive().files))
    //testImplementation("androidx.test.ext:junit:$versions.androidx_junit")
    testImplementation("org.robolectric:robolectric:4.14")
}

afterEvaluate {
    fun CharSequence.capitalized() =
        toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    android.applicationVariants.forEach { variant ->
        val productFlavor = variant.productFlavors.joinToString("") { it.name.capitalized() }
        val buildType = variant.buildType.name.capitalized()
        tasks["generate${productFlavor}${buildType}Assets"].dependsOn(tasks["cargoBuild"])

        // Don't merge the jni lib folders until after the Rust libraries have been built.
        tasks["merge${productFlavor}${buildType}JniLibFolders"].dependsOn(tasks["cargoBuild"])

        // For unit tests.
        tasks["process${productFlavor}${buildType}UnitTestJavaRes"].dependsOn(tasks["cargoBuild"])
    }
}