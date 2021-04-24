# 0.8.5

- Allow to use `+nightly`, etc, with `cargo { rustupChannel = "..." }`. Fixes #24.
- Allow to set `cargo { (cargo|python|rustc)Command = "..." }`. Fixes #48.

# 0.8.4

- The plugin tries to interoperate with Rust bindgen out of the box by setting `CLANG_PATH`.
- We no longer invoke `cargo` for the Gradle `clean` target.

# 0.8.3

- Plugin now supports using prebuilt NDK toolchains.

# 0.8.2

- Avoid passing `--target` to cargo for the default target.
- The `exec` callback is now invoked as late as possible.
- The `CARGO_TARGET_DIR` environment variable should now be respected, if it is set.
- Various parts of the plugin's documentation have been improved.

# 0.8.1

- Added `extraCargoBuildArguments`.

# 0.8.0

- **breaking** Further split "win32-x86-64" into "win32-x86-64-{gnu,msvc}".
- Fixed bug with DLL libraries in with JNA: expect "foo.dll" instead
  of "libfoo.dll".

# 0.7.0

- Added per-target pass-through variables.
- Separated "default" target into multiple Desktop targets:
  "linux-x86-64, "darwin", "win32-x86-64".
