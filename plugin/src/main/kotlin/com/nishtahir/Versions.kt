package com.nishtahir

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.util.GradleVersion

@Serializable
private data class VersionsSerial(
    val version: String,
    val supportedVersions: Map<String, List<String>>
)

@OptIn(ExperimentalSerializationApi::class)
object Versions {
    val PLUGIN_VERSION: VersionNumber
    val SUPPORTED_VERSIONS_MATRIX: Map<VersionNumber, Set<GradleVersion>>
    val SUPPORTED_ANDROID_VERSIONS: Set<VersionNumber>
    val SUPPORTED_GRADLE_VERSIONS: Set<GradleVersion>

    init {
        val serial = Json.decodeFromStream<VersionsSerial>(this::class.java.getResourceAsStream("/versions.json")!!)
        PLUGIN_VERSION = VersionNumber.parse(serial.version)

        val matrix = serial.supportedVersions.map { entry ->
            android(entry.key) to entry.value.map { gradle(it) }.toSet()
        }.toMap()
        SUPPORTED_VERSIONS_MATRIX = matrix
        SUPPORTED_ANDROID_VERSIONS = matrix.keys
        SUPPORTED_GRADLE_VERSIONS = matrix.values.flatten().toSet()
    }

    fun earliestMaybeSupportedAndroidVersion(): VersionNumber {
        val earliestSupported = SUPPORTED_ANDROID_VERSIONS.min()
        return VersionNumber(
            earliestSupported.major,
            earliestSupported.minor,
            0,
            "alpha"
        )
    }

    fun latestAndroidVersion() = SUPPORTED_ANDROID_VERSIONS.max()
}

fun android(version: String) = VersionNumber.parse(version)
fun gradle(version: String) = GradleVersion.version(version)
