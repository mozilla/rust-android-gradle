#!/usr/bin/env bash

# Invoke linker-wrapper.py with the correct Python command.
"${RUST_ANDROID_GRADLE_PYTHON_COMMAND}" "${RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY}" "$@"
