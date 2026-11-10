package com.nishtahir

import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.util.GradleVersion
import java.lang.module.ModuleDescriptor.Version as VersionNumber

@CompileStatic(TypeCheckingMode.SKIP)
class Versions {
    static final VersionNumber PLUGIN_VERSION;
    static final Multimap<VersionNumber, GradleVersion> SUPPORTED_VERSIONS_MATRIX

    static {
        def versions = new JsonSlurper().parse(Versions.classLoader.getResource("versions.json"))
        PLUGIN_VERSION = VersionNumber.parse(versions.version)

        def builder = ImmutableMultimap.<VersionNumber, GradleVersion>builder()
        versions.supportedVersions.each { String androidVersion, List<String> gradleVersions ->
            builder.putAll(android(androidVersion), gradleVersions.collect { gradle(it) })
        }
        SUPPORTED_VERSIONS_MATRIX = builder.build()
    }

    static VersionNumber android(String version) {
        VersionNumber.parse(version)
    }

    static GradleVersion gradle(String version) {
        GradleVersion.version(version)
    }

    static VersionNumber latestAndroidVersion() {
        return SUPPORTED_VERSIONS_MATRIX.keySet().max()
    }
}
