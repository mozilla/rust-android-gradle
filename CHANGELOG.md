# 0.8.1
- Added the ability to select rust targets architectures using the `RUST_ANDROID_GRADLE_TARGETS` environment variable.

# 0.8.0

- **breaking** Further split "win32-x86-64" into "win32-x86-64-{gnu,msvc}".
- Fixed bug with DLL libraries in with JNA: expect "foo.dll" instead
  of "libfoo.dll".

# 0.7.0

- Added per-target pass-through variables.
- Separated "default" target into multiple Desktop targets:
  "linux-x86-64, "darwin", "win32-x86-64".
