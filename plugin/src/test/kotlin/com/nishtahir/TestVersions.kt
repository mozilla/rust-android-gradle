package com.nishtahir

object TestVersions {
    val allCandidateTestVersions = run {
        val testedVersion = System.getProperty("org.gradle.android.testVersion")
        if (!testedVersion.isNullOrEmpty()) {
            val parsedVersion = VersionNumber.parse(testedVersion)
            Versions.SUPPORTED_VERSIONS_MATRIX.filter {
                it.key == parsedVersion
            }
        } else {
            Versions.SUPPORTED_VERSIONS_MATRIX
        }
    }

    val latestAndroidVersionForCurrentJDK = run {
        val current = System.getProperty("java.version")
        val version7 = VersionNumber.parse("7.0.0-alpha01")
        if (current.startsWith("1.")) {
            allCandidateTestVersions.keys
                .filter { it < version7 }
                .maxOrNull()!!
        } else {
            allCandidateTestVersions.keys.maxOrNull()!!
        }
    }

    val latestGradleVersion = allCandidateTestVersions.values.flatten().maxOrNull()!!

    val latestAndroidVersions = allCandidateTestVersions.keys
        .map { getLatestVersionForAndroid("${it.major}.${it.minor}") }

    fun latestSupportedGradleVersionFor(androidVersion: VersionNumber) =
        allCandidateTestVersions.entries.find {
            it.key.major == androidVersion.major && it.key.minor == androidVersion.minor
        }?.value?.maxOrNull()!!

    val latestKotlinVersion = VersionNumber.parse("1.9.25")

    private fun getLatestVersionForAndroid(version: String): VersionNumber {
        val number = VersionNumber.parse(version)
        return allCandidateTestVersions.keys
            .filter { it.major == number.major && it.minor == number.minor }
            .maxOrNull()!!
    }
}