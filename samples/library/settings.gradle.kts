pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "me.sigptr.rust-android") {
                useModule("me.sigptr.rust-android:rust-android:+")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("me.sigptr.rust-android:rust-android")).using(project(":plugin"))
    }
}