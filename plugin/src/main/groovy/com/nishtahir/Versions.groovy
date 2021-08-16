package com.nishtahir

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.ImmutableSortedSet
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion
import org.gradle.util.VersionNumber

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final VersionNumber PLUGIN_VERSION;
    static final Set<GradleVersion> SUPPORTED_GRADLE_VERSIONS
    static final Set<VersionNumber> SUPPORTED_ANDROID_VERSIONS
    static final Multimap<VersionNumber, GradleVersion> SUPPORTED_VERSIONS_MATRIX

    static {
        def versions = new JsonSlurper().parse(Versions.classLoader.getResource("versions.json"))
        PLUGIN_VERSION = VersionNumber.parse(versions.version)

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        versions.supportedVersions.each { String androidVersion, List<String> gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
        def matrix = builder.build()

        SUPPORTED_VERSIONS_MATRIX = matrix
        SUPPORTED_ANDROID_VERSIONS = ImmutableSortedSet.copyOf(matrix.keySet())
        SUPPORTED_GRADLE_VERSIONS = ImmutableSortedSet.copyOf(matrix.values())
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber earliestMaybeSupportedAndroidVersion() {
        VersionNumber earliestSupported = SUPPORTED_ANDROID_VERSIONS.min()
        // "alpha" is lower than null
        return new VersionNumber(earliestSupported.major, earliestSupported.minor, 0, "alpha")
    }

    static VersionNumber latestAndroidVersion() {
        return SUPPORTED_ANDROID_VERSIONS.max()
    }
}
