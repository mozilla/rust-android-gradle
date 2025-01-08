plugins {
    alias(libs.plugins.gradle.versions)
    alias(libs.plugins.version.catalog.update)
}

task("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

