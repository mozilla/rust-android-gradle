package com.nishtahir

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import org.gradle.util.GradleVersion
import java.lang.module.ModuleDescriptor.Version as VersionNumber


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
        return allCandidateTestVersions.keySet().max()
    }

    static GradleVersion latestSupportedGradleVersionFor(VersionNumber androidVersion) {
        return allCandidateTestVersions.get(androidVersion).max()
    }
}
