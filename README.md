# Rust Android Gradle Plugin

Cross compiles rust cargo projects for Android

# Usage

To begin you must first install the rust toolchains for your target platforms.

```
rustup target add armv7-linux-androideabi   # for arm
rustup target add i686-linux-android        # for x86
...
```

Next add the `cargo` configuration to android project. Point to your cargo project using `module` and add targets.
Currently supported targets are `arm`, `arm64` and `x86`, `x86_64`.

```
cargo {
    module = "../rust"
    targets = ["arm", "x86"]
}

```

Run the `cargoBuild` task to cross compile

```
./gradlew cargoBuild
```

Generated static libraries will be added to your android `jniLibs` source-sets.

# Development

At top-level, the `publish` Gradle task updates the Maven repository
under `samples`:

```
$ ./gradlew publish
...
$ ls -al samples/maven-repo/org/mozilla/rust-android-gradle/org.mozilla.rust-android-gradle.gradle.plugin/0.4.0/org.mozilla.rust-android-gradle.gradle.plugin-0.4.0.pom
-rw-r--r--  1 nalexander  staff  670 18 Sep 10:09
samples/maven-repo/org/mozilla/rust-android-gradle/org.mozilla.rust-android-gradle.gradle.plugin/0.4.0/org.mozilla.rust-android-gradle.gradle.plugin-0.4.0.pom
```

## Sample projects

To run the sample projects:

```
$ ./gradlew -p samples/library :assembleDebug
...
$ ls -al samples/library/build//outputs/aar/library-debug.aar
-rw-r--r--  1 nalexander  staff  8926315 18 Sep 10:22 samples/library/build//outputs/aar/library-debug.aar
```

## Real projects

To test in a real project, use the local Maven repository in your `build.gradle`, like:

```
buildscript {
    repositories {
        maven {
            url "file:///Users/nalexander/Mozilla/rust-android-gradle/samples/maven-repo"
        }
    }

    dependencies {
        classpath 'org.mozilla.rust-android-gradle:plugin:0.3.0'
    }
}
```
