package com.nishtahir

import org.gradle.util.GradleVersion

class TestVersions {
    static Map<VersionNumber, Set<GradleVersion>> getAllCandidateTestVersions() {
        def testedVersion = System.getProperty('org.gradle.android.testVersion')
        if (testedVersion) {
            return Versions.@INSTANCE.SUPPORTED_VERSIONS_MATRIX.entrySet().findAll {
                it.key == VersionNumber.@Companion.parse(testedVersion)
            }.collectEntries()
        } else {
            return Versions.@INSTANCE.SUPPORTED_VERSIONS_MATRIX
        }
    }

    static VersionNumber latestAndroidVersionForCurrentJDK() {
        String currentJDKVersion = System.getProperty("java.version")
        if (currentJDKVersion.startsWith("1.")) {
            return allCandidateTestVersions.keySet().findAll {it < VersionNumber.@Companion.parse("7.0.0-alpha01")}.max()
        }
        return allCandidateTestVersions.keySet().max()
    }

    static GradleVersion latestGradleVersion() {
        return allCandidateTestVersions.values().max()
    }

    static GradleVersion latestSupportedGradleVersionFor(String androidVersion) {
        return latestSupportedGradleVersionFor(VersionNumber.@Companion.parse(androidVersion))
    }

    static GradleVersion latestSupportedGradleVersionFor(VersionNumber androidVersion) {
        return allCandidateTestVersions.find { it.key.major == androidVersion.major && it.key.minor == androidVersion.minor }?.value?.max()
    }

    static VersionNumber getLatestVersionForAndroid(String version) {
        VersionNumber versionNumber = VersionNumber.@Companion.parse(version)
        return allCandidateTestVersions.keySet().findAll { it.major == versionNumber.major && it.minor == versionNumber.minor }?.max()
    }

    static List<VersionNumber> getLatestAndroidVersions() {
        def minorVersions = allCandidateTestVersions.keySet().collect { "${it.major}.${it.minor}" }
        return minorVersions.collect { getLatestVersionForAndroid(it) }
    }
}
