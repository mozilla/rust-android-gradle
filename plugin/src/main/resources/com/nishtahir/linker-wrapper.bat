@echo off
if defined SHELL (
    where /q cygpath
    if ERRORLEVEL 1 (
        goto WINSHELL
    )
    for /f %%i in ('cygpath -w %SHELL%') do (
        if exist "%%i" (
            "%%i" -c 'RUST_ANDROID_GRADLE_CC="$(cygpath -u "${RUST_ANDROID_GRADLE_CC%%.*}")";^
                      exec "$(cygpath -u "%~dp0linker-wrapper.sh")" "%*"'
            exit !errorlevel!
        )
        exit 1
    )
)
:WINSHELL
@echo on
"%RUST_ANDROID_GRADLE_PYTHON_COMMAND%" "%RUST_ANDROID_GRADLE_LINKER_WRAPPER_PY%" %*
