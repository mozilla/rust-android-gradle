# Rust Android Gradle Plugin

Cross compile Rust Cargo projects for Android targets.


<p align="left">
    <a alt="Version badge" href="https://plugins.gradle.org/plugin/org.mozilla.rust-android-gradle.rust-android">
        <img src="https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/org/mozilla/rust-android-gradle/rust-android/org.mozilla.rust-android-gradle.rust-android.gradle.plugin/maven-metadata.xml.svg?label=rust-android-gradle&colorB=brightgreen" /></a>
</p>

# Usage

Add the plugin to your root `build.gradle`, like:

```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2/"
        }
    }
    dependencies {
        classpath 'io.github.emakryo.rust-android-gradle:plugin:0.1.0'
    }
}
```

or 

```groovy
buildscript {
    //...
}

plugins {
    id "io.github.emakryo.rust-android-gradle.rust-android" version "0.1.0"
}
```

In your *project's* build.gradle, `apply plugin` and add the `cargo` configuration:

```groovy
android { ... }

apply plugin: 'io.github.emakryo.rust-android-gradle.rust-android'

cargo {
    module  = "../rust"       // Or whatever directory contains your Cargo.toml
    libname = "rust"          // Or whatever matches Cargo.toml's [package] name.
    targets = ["arm", "x86"]  // See bellow for a longer list of options
}
```

Install the rust toolchains for your target platforms:

```sh
rustup target add armv7-linux-androideabi   # for arm
rustup target add i686-linux-android        # for x86
rustup target add aarch64-linux-android     # for arm64
rustup target add x86_64-linux-android      # for x86_64
rustup target add x86_64-unknown-linux-gnu  # for linux-x86-64
rustup target add x86_64-apple-darwin       # for darwin x86_64 (if you have an Intel MacOS)
rustup target add aarch64-apple-darwin      # for darwin arm64 (if you have a M1 MacOS)
rustup target add x86_64-pc-windows-gnu     # for win32-x86-64-gnu
rustup target add x86_64-pc-windows-msvc    # for win32-x86-64-msvc
...
```

Finally, run the `cargoBuild` task to cross compile:
```sh
./gradlew cargoBuild
```
Or add it as a dependency to one of your other build tasks, to build your rust code when you normally build your project:
```gradle
tasks.whenTaskAdded { task ->
    if ((task.name == 'javaPreCompileDebug' || task.name == 'javaPreCompileRelease')) {
        task.dependsOn 'cargoBuild'
    }
}
```

## Configuration

The `cargo` Gradle configuration accepts many options.

### Linking Java code to native libraries

Generated libraries will be added to the Android `jniLibs` source-sets, when correctly referenced in
the `cargo` configuration through the `libname` and/or `targetIncludes` options.  The latter
defaults to `["lib${libname}.so", "lib${libname}.dylib", "{$libname}.dll"]`, so the following configuration will
include all `libbackend` libraries generated in the Rust project in `../rust`:

```
cargo {
    module = "../rust"
    libname = "backend"
}
```

Now, Java code can reference the native library using, e.g.,

```java
static {
    System.loadLibrary("backend");
}
```

### Native `apiLevel`

The [Android NDK](https://developer.android.com/ndk/guides/stable_apis) also fixes an API level,
which can be specified using the `apiLevel` option.  This option defaults to the minimum SDK API
level.  As of API level 21, 64-bit builds are possible; and conversely, the `arm64` and `x86_64`
targets require `apiLevel >= 21`.

### Cargo release profile

The `profile` option selects between the `--debug` and `--release` profiles in `cargo`.  *Defaults
to `debug`!*

### Extension reference

### module

The path to the Rust library to build with Cargo; required.  `module` can be absolute; if it is not,
it is interpreted as a path relative to the Gradle `projectDir`.

```groovy
cargo {
    // Note: path is either absolute, or relative to the gradle project's `projectDir`.
    module = '../rust'
}
```

### libname

The library name produced by Cargo; required.

`libname` is used to determine which native libraries to include in the produced AARs and/or APKs.
See also [`targetIncludes`](#targetincludes).

`libname` is also used to determine the ELF SONAME to declare in the Android libraries produced by
Cargo.  Different versions of the Android system linker
[depend on the ELF SONAME](https://android-developers.googleblog.com/2016/06/android-changes-for-ndk-developers.html).

In `Cargo.toml`:

```toml
[lib]
name = "test"
```

In `build.gradle`:

```groovy
cargo {
    libname = 'test'
}
```

### targets

A list of Android targets to build with Cargo; required.

Valid targets for **Android** are:

```
'arm',
'arm64',
'x86',
'x86_64'
```
Valid targets for **Desktop** are:
```
'linux-x86-64',
'darwin-x86-64',
'darwin-aarch64',
'win32-x86-64-gnu',
'win32-x86-64-msvc'
```

The desktop targets are useful for testing native code in Android unit tests that run on the host,
not on the target device.  Better support for this feature is
[planned](https://github.com/ncalexan/rust-android-gradle/issues/13).

```groovy
cargo {
    targets = ['arm', 'x86', 'linux-x86-64']
}
```

### prebuiltToolchains

When set to `true` (which requires NDK version 19+), use the prebuilt toolchains bundled with the
NDK. When set to `false`, generate per-target architecture standalone NDK toolchains using
`make_standalone_toolchain.py`.  When unset, use the prebuilt toolchains if the NDK version is 19+,
and fall back to generated toolchains for older NDK versions.

Defaults to `null`.

```groovy
cargo {
    prebuiltToolchains = true
}
```

### verbose

When set, execute `cargo build` with or without the `--verbose` flag.  When unset, respect the
Gradle log level: execute `cargo build` with or without the `--verbose` flag according to whether
the log level is at least `INFO`.  In practice, this makes `./gradlew ... --info` (and `./gradlew
... --debug`) execute `cargo build --verbose ...`.

Defaults to `null`.

```groovy
cargo {
    verbose = true
}
```

### profile

The Cargo [release profile](https://doc.rust-lang.org/book/second-edition/ch14-01-release-profiles.html#customizing-builds-with-release-profiles) to build.

Defaults to `"debug"`.

```groovy
cargo {
    profile = 'release'
}
```

### features

Set the Cargo [features](https://doc.rust-lang.org/cargo/reference/manifest.html#the-features-section).

Defaults to passing no flags to `cargo`.

To pass `--all-features`, use
```groovy
cargo {
    features {
        all()
    }
}
```

To pass an optional list of `--features`, use
```groovy
cargo {
    features {
        defaultAnd("x")
        defaultAnd("x", "y")
    }
}
```

To pass `--no-default-features`, and an optional list of replacement `--features`, use
```groovy
cargo {
    features {
        noDefaultBut()
        noDefaultBut("x")
        noDefaultBut "x", "y"
    }
}
```

### targetDirectory

The target directory into which Cargo writes built outputs. You will likely need to specify this
if you are using a [cargo virtual workspace](https://doc.rust-lang.org/book/ch14-03-cargo-workspaces.html),
as our default will likely fail to locate the correct target directory.

Defaults to `${module}/target`.  `targetDirectory` can be absolute; if it is not, it is interpreted
as a path relative to the Gradle `projectDir`.

Note that if `CARGO_TARGET_DIR` (see https://doc.rust-lang.org/cargo/reference/environment-variables.html)
is specified in the environment, it takes precedence over `targetDirectory`, as cargo will output
all build artifacts to it, regardless of what is being built, or where it was invoked.

You may also override `CARGO_TARGET_DIR` variable by setting `rust.cargoTargetDir` in
`local.properties`, however it seems very unlikely that this will be useful, as we don't pass this
information to cargo itself. That said, it can be used to control where we search for the built
library on a per-machine basis.

```groovy
cargo {
    // Note: path is either absolute, or relative to the gradle project's `projectDir`.
    targetDirectory = 'path/to/workspace/root/target'
}
```

### targetIncludes

Which Cargo outputs to consider JNI libraries.

Defaults to `["lib${libname}.so", "lib${libname}.dylib", "{$libname}.dll"]`.

```groovy
cargo {
    targetIncludes = ['libnotlibname.so']
}
```

### apiLevel

The Android NDK API level to target.  NDK API levels are not the same as SDK API versions; they are
updated less frequently.  For example, SDK API versions 18, 19, and 20 all target NDK API level 18.

Defaults to the minimum SDK version of the Android project's default configuration.

```groovy
cargo {
    apiLevel = 21
}
```

You may specify the API level per target in `targets` using the `apiLevels` option. At most one of
`apiLevel` and `apiLevels` may be specified. `apiLevels` must have an entry for each target in
`targets`.

```groovy
cargo {
    targets = ["arm", "x86_64"]
    apiLevels = [
        "arm": 16,
        "x86_64": 21,
    ]
}
```

### extraCargoBuildArguments

Sometimes, you need to do things that the plugin doesn't anticipate.  Use `extraCargoBuildArguments`
to append a list of additional arguments to each `cargo build` invocation.

```groovy
cargo {
    extraCargoBuildArguments = ['a', 'list', 'of', 'strings']
}
```

### exec

This is a callback taking the `ExecSpec` we're going to use to invoke `cargo build`, and
the relevant toolchain. It's called for each invocation of `cargo build`. This generally
is useful for the following scenarios:

1. Specifying target-specific environment variables.
1. Adding target-specific flags to the command line.
1. Removing/modifying environment variables or command line options the rust-android-gradle plugin would
   provide by default.

```groovy
cargo {
    exec { spec, toolchain ->
        if (toolchain.target != "x86_64-apple-darwin") {
            // Don't statically link on macOS desktop builds, for some
            // entirely hypothetical reason.
            spec.environment("EXAMPLELIB_STATIC", "1")
        }
    }
}
```

### Build types

The plugin supports building for multiple build types.  The build types are compatible with the
[android plugin's build types](https://developer.android.com/build/build-variants#build-types),
and are specified in the `buildTypes` block. Any configuration above can be specified per build.
For configuration value of `profile`, `dev` profile is used for debuggable build types (e.g. `debug` build type),
and `release` is used otherwise (e.g. `release` build type).
`buildTypes` block is useful to specify different features for different build types.

```groovy
cargo {
    features {
        defaultAnd "x"
    }
    buildTypes {
        debug {
            features {
                defaultAnd "y"
            }
        }
        release {
            features {
                noDefaultBut "z"
            }
        }
    }
}
```

## Specifying NDK toolchains

The plugin can either use prebuilt NDK toolchain binaries, or search for (and if missing, build)
NDK toolchains as generated by `make_standalone_toolchain.py`.

A prebuilt NDK toolchain will be used if:
1. `rust.prebuiltToolchain=true` in the per-(multi-)project `${rootDir}/local.properties`
1. `prebuiltToolchain=true` in the `cargo { ... }` block (if not overridden by `local.properties`)
1. The discovered NDK is version 19 or higher (if not overridden per above)

The toolchains are rooted in a single Android NDK toolchain directory.  In order of preference, the
toolchain root directory is determined by:

1. `rust.androidNdkToolchainDir` in the per-(multi-)project `${rootDir}/local.properties`
1. the environment variable `ANDROID_NDK_TOOLCHAIN_DIR`
1. `${System.getProperty(java.io.tmpdir)}/rust-android-ndk-toolchains`

Note that the Java system property `java.io.tmpdir` is not necessarily `/tmp`, including on macOS hosts.

Each target architecture toolchain is named like `$arch-$apiLevel`: for example, `arm-16` or `arm64-21`.

## Specifying local targets

When developing a project that consumes `rust-android-gradle` locally, it's often convenient to
temporarily change the set of Rust target architectures.  In order of preference, the plugin
determines the per-project targets by:

1. `rust.targets.${project.Name}` for each project in `${rootDir}/local.properties`
1. `rust.targets` in `${rootDir}/local.properties`
1. the `cargo { targets ... }` block in the per-project `build.gradle`

The targets are split on `','`.  For example:

```
rust.targets.library=linux-x86-64
rust.targets=arm,linux-x86-64,darwin
```

## Specifying paths to sub-commands (Python, Cargo, and Rustc)

The plugin invokes Python, Cargo and Rustc.  In order of preference, the plugin determines what command to invoke for Python by:

1. the value of `cargo { pythonCommand = "..." }`, if non-empty
1. `rust.pythonCommand` in `${rootDir}/local.properties`
1. the environment variable `RUST_ANDROID_GRADLE_PYTHON_COMMAND`
1. the default, `python`

In order of preference, the plugin determines what command to invoke for Cargo by:

1. the value of `cargo { cargoCommand = "..." }`, if non-empty
1. `rust.cargoCommand` in `${rootDir}/local.properties`
1. the environment variable `RUST_ANDROID_GRADLE_CARGO_COMMAND`
1. the default, `cargo`

In order of preference, the plugin determines what command to invoke for `rustc` by:

1. the value of `cargo { rustcCommand = "..." }`, if non-empty
1. `rust.rustcCommand` in `${rootDir}/local.properties`
1. the environment variable `RUST_ANDROID_GRADLE_RUSTC_COMMAND`
1. the default, `rustc`

(Note that failure to locate `rustc` is not fatal, however it may result in rebuilding the code more often than is necessary).

Paths must be host operating system specific.  For example, on Windows:

```properties
rust.pythonCommand=c:\Python27\bin\python
```

On Linux,
```shell
env RUST_ANDROID_GRADLE_CARGO_COMMAND=$HOME/.cargo/bin/cargo ./gradlew ...
```

## Specifying Rust channel

Rust is released to three different "channels": stable, beta, and nightly (see
https://rust-lang.github.io/rustup/concepts/channels.html).  The `rustup` tool, which is how most
people install Rust, allows multiple channels to be installed simultaneously and to specify which
channel to use by invoking `cargo +channel ...`.

In order of preference, the plugin determines what channel to invoke `cargo` with by:

1. the value of `cargo { rustupChannel = "..." }`, if non-empty
1. `rust.rustupChannel` in `${rootDir}/local.properties`
1. the environment variable `RUST_ANDROID_GRADLE_RUSTUP_CHANNEL`
1. the default, no channel specified (which `cargo` installed via `rustup` generally defaults to the
   `stable` channel)

The channel should be recognized by `cargo` installed via `rustup`, i.e.:
- `"stable"`
- `"beta"`
- `"nightly"`

A single leading `'+'` will be stripped, if present.

(Note that Cargo installed by a method other than `rustup` will generally not understand `+channel`
and builds will likely fail.)

## Passing arguments to cargo

The plugin passes project properties named like `RUST_ANDROID_GRADLE_target_..._KEY=VALUE` through
to the Cargo invocation for the given Rust `target` as `KEY=VALUE`.  Target should be upper-case
with "-" replaced by "_".  (See [the links from this Cargo issue](https://github.com/rust-lang/cargo/issues/5690).) So, for example,

```groovy
project.RUST_ANDROID_GRADLE_I686_LINUX_ANDROID_FOO=BAR
```
and
```shell
./gradlew -PRUST_ANDROID_GRADLE_ARMV7_LINUX_ANDROIDEABI_FOO=BAR ...
```
and
```
env ORG_GRADLE_PROJECT_RUST_ANDROID_GRADLE_ARMV7_LINUX_ANDROIDEABI_FOO=BAR ./gradlew ...
```
all set `FOO=BAR` in the `cargo` execution environment (for the "armv7-linux-androideabi` Rust
target, corresponding to the "x86" target in the plugin).

# Development

At top-level, the `publish` Gradle task updates the Maven repository
under `build/local-repo`:

```
$ ./gradlew publish
...
$ ls -al build/local-repo/org/mozilla/rust-android-gradle/org.mozilla.rust-android-gradle.gradle.plugin/0.4.0/org.mozilla.rust-android-gradle.gradle.plugin-0.4.0.pom
-rw-r--r--  1 nalexander  staff  670 18 Sep 10:09
build/local-repo/org/mozilla/rust-android-gradle/org.mozilla.rust-android-gradle.gradle.plugin/0.4.0/org.mozilla.rust-android-gradle.gradle.plugin-0.4.0.pom
```

## Sample projects

The easiest way to get started is to run the sample projects.  The sample projects have dependency
substitutions configured so that changes made to `plugin/` are reflected in the sample projects
immediately.

```
$ ./gradlew -p samples/library :assembleDebug
...
$ file samples/library/build/outputs/aar/library-debug.aar
samples/library/build/outputs/aar/library-debug.aar: Zip archive data, at least v1.0 to extract
```

```
$ ./gradlew -p samples/app :assembleDebug
...
$ file samples/app/build/outputs/apk/debug/app-debug.apk
samples/app/build/outputs/apk/debug/app-debug.apk: Zip archive data, at least v?[0] to extract
```

## Testing Local changes

An easy way to locally test changes made in this plugin is to simply add this to your project's `settings.gradle`:

```gradle
// Switch this to point to your local plugin dir
includeBuild('../rust-android-gradle') {
    dependencySubstitution {
        // As required.
        substitute module('io.github.emakryo.rust-android-gradle:plugin') with project(':plugin')
    }
}
```

# Publishing

## Automatically via the Bump version Github Actions workflow

You will need to be a collaborator.  First, manually invoke the [Bump version Github Actions
workflow](https://github.com/mozilla/rust-android-gradle/actions/workflows/bump.yml).  Specify a
version (like "x.y.z", without quotes) and a single line changelog entry.  (This entry will have a
dash prepended, so that it would look normal in a list.  This is working around [the lack of a
multi-line input in Github
Actions](https://github.community/t/multiline-inputs-for-workflow-dispatch/163906).)  This will push
a preparatory commit updating version numbers and the changelog like [this
one](https://github.com/mozilla/rust-android-gradle/commit/2a637d1797a5d0b5063b8d2f0a3d4a4938511154),
and make a **draft** Github Release with a name like `vx.y.z`.  After verifying that tests pass,
navigate to [the releases panel](https://github.com/mozilla/rust-android-gradle/releases) and edit
the release, finally pressing "Publish release".  The release Github workflow will build and publish
the plugin, although it may take some days for it to be reflected on the Gradle plugin portal.

## By hand

You will need credentials to publish to the [Gradle plugin portal](https://plugins.gradle.org/) in
the appropriate place for the [`plugin-publish`](https://plugins.gradle.org/docs/publish-plugin) to
find them.  Usually, that's in `~/.gradle/gradle.properties`.

At top-level, the `publishPlugins` Gradle task publishes the plugin for consumption:

```
$ ./gradlew publishPlugins
...
Publishing plugin org.mozilla.rust-android-gradle.rust-android version 0.8.1
Publishing artifact build/libs/plugin-0.8.1.jar
Publishing artifact build/libs/plugin-0.8.1-sources.jar
Publishing artifact build/libs/plugin-0.8.1-javadoc.jar
Publishing artifact build/publish-generated-resources/pom.xml
Activating plugin org.mozilla.rust-android-gradle.rust-android version 0.8.1
```

## Real projects

To test in a real project, use the local Maven repository in your `build.gradle`, like:

```gradle
buildscript {
    repositories {
        maven {
            url "file:///Users/nalexander/Mozilla/rust-android-gradle/build/local-repo"
        }
    }

    dependencies {
        classpath 'io.github.emakryo.rust-android-gradle:plugin:0.9.0'
    }
}
```
