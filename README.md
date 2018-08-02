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
