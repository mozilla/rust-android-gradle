package com.nishtahir

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber


class TestVersions {
    static Multimap<VersionNumber, GradleVersion> getAllCandidateTestVersions() {
        def testedVersion = System.getProperty('org.gradle.android.testVersion')
        if (testedVersion) {
            return ImmutableMultimap.copyOf(Versions.SUPPORTED_VERSIONS_MATRIX.entries().findAll {it.key == VersionNumber.parse(testedVersion) })
        } else {
            return Versions.SUPPORTED_VERSIONS_MATRIX
        }
    }

    static VersionNumber latestAndroidVersionForCurrentJDK() {
        String currentJDKVersion = System.getProperty("java.version");
        if (currentJDKVersion.startsWith("1.")) {
            return allCandidateTestVersions.keySet().findAll {it < VersionNumber.parse("7.0.0-alpha01")}.max()
        }
        return allCandidateTestVersions.keySet().max()
    }

    static GradleVersion latestGradleVersion() {
        return allCandidateTestVersions.values().max()
    }

    static GradleVersion latestSupportedGradleVersionFor(String androidVersion) {
        return latestSupportedGradleVersionFor(VersionNumber.parse(androidVersion))
    }

    static GradleVersion latestSupportedGradleVersionFor(VersionNumber androidVersion) {
        return allCandidateTestVersions.asMap().find {it.key.major == androidVersion.major && it.key.minor == androidVersion.minor }?.value?.max()
    }

    static VersionNumber getLatestVersionForAndroid(String version) {
        VersionNumber versionNumber = VersionNumber.parse(version)
        return allCandidateTestVersions.keySet().findAll { it.major == versionNumber.major && it.minor == versionNumber.minor }?.max()
    }

    static List<VersionNumber> getLatestAndroidVersions() {
        def minorVersions = allCandidateTestVersions.keySet().collect { "${it.major}.${it.minor}" }
        return minorVersions.collect { getLatestVersionForAndroid(it) }
    }
}
