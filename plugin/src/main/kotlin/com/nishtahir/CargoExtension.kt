package com.nishtahir

open class CargoExtension {
    var module: String = ""
    var targets: List<String> = emptyList()

    /**
     * The Cargo [release profile](https://doc.rust-lang.org/book/second-edition/ch14-01-release-profiles.html#customizing-builds-with-release-profiles) to build.
     *
     * Defaults to `"debug"`.
     */
    var profile: String = "debug"
}
