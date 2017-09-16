# Rust Android Gradle Plugin

Cross compiles rust cargo projects for Android

# Usage

To begin you must first install the rust toolchains for your target platforms.

```
rustup target add arm-linux-androideabi
```

Next add the `cargo` configuration to android project. Point to your cargo project using `module` and add targets.
Currently supported targets are `arm`, `mips`, and `x86`.

```
cargo {
    module = "../rust"
    targets = ["arm"]
}

```

Run the `cargoBuild` task to cross compile

```
./gradlew cargoBuild
```

Generated static libraries will be added to your android `jniLibs` sourcesets.

