/*
 * Copyright 2012 the original author or authors. (Gradle)
 * Copyright 2025 Thomas Bell. (modification)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This source file is based on the org.gradle.util.VersionNumber class previously
 * distributed as part of Gradle through version 8.x. The modifications amount to
 * conformance to Kotlin conventions and a removal of deprecation notices.
 */
package com.nishtahir

data class VersionNumber(
    val major: Int,
    val minor: Int,
    val micro: Int,
    val patch: Int,
    val qualifier: String,
    private val scheme: Scheme
): Comparable<VersionNumber> {
    companion object {
        private val DEFAULT_SCHEME = object : AbstractScheme(3) {
            override fun format(versionNumber: VersionNumber): String =
                String.format(
                    "%d.%d.%d%s",
                    versionNumber.major,
                    versionNumber.minor,
                    versionNumber.micro,
                    if (versionNumber.qualifier.isEmpty()) { "" } else { "-" + versionNumber.qualifier }
                )
        }
        private val PATCH_SCHEME = object : AbstractScheme(4) {
            override fun format(versionNumber: VersionNumber): String =
                String.format(
                    "%d.%d.%d.%d%s",
                    versionNumber.major,
                    versionNumber.minor,
                    versionNumber.micro,
                    versionNumber.patch,
                    if (versionNumber.qualifier.isEmpty()) { "" } else { "-" + versionNumber.qualifier }
                )
        }
        val UNKNOWN: VersionNumber = version(0)

        fun version(major: Int) = version(major, 0)
        fun version(major: Int, minor: Int) = VersionNumber(major, minor, 0, 0, "", DEFAULT_SCHEME)

        fun scheme(): Scheme = DEFAULT_SCHEME
        fun withPatchNumber(): Scheme = PATCH_SCHEME

        fun parse(versionString: String) = DEFAULT_SCHEME.parse(versionString)
    }

    constructor(major: Int, minor: Int, micro: Int, qualifier: String) :
            this(major, minor, micro, 0, qualifier, DEFAULT_SCHEME)

    constructor(major: Int, minor: Int, micro: Int, patch: Int, qualifier: String) :
            this(major, minor, micro, patch, qualifier, PATCH_SCHEME)

    fun baseVersion() = VersionNumber(major, minor, micro, patch, "", scheme)

    override fun compareTo(other: VersionNumber): Int {
        if (major != other.major) {
            return major - other.major
        }
        if (minor != other.minor) {
            return minor - other.minor
        }
        if (micro != other.micro) {
            return micro - other.micro
        }
        if (patch != other.patch) {
            return patch - other.patch
        }

        return qualifier.lowercase().compareTo(other.qualifier.lowercase())
    }

    override fun toString(): String = scheme.format(this)

    interface Scheme {
        fun parse(value: String?): VersionNumber
        fun format(versionNumber: VersionNumber): String
    }

    private abstract class AbstractScheme(val depth: Int): Scheme {
        override fun parse(value: String?): VersionNumber {
            if (value.isNullOrEmpty()) {
                return UNKNOWN
            }

            val scanner = Scanner(value)

            if (!scanner.hasDigit()) {
                return UNKNOWN
            }

            val major = scanner.scanDigit()
            var minor = 0
            var micro = 0
            var patch = 0
            if (scanner.isSeparatorAndDigit('.')) {
                scanner.skipSeparator()
                minor = scanner.scanDigit()
                if (scanner.isSeparatorAndDigit('.')) {
                    scanner.skipSeparator()
                    micro = scanner.scanDigit()
                    if (depth > 3 && scanner.isSeparatorAndDigit('.', '_')) {
                        scanner.skipSeparator()
                        patch = scanner.scanDigit()
                    }
                }
            }

            if (scanner.isEnd()) {
                return VersionNumber(major, minor, micro, patch, "", this)
            }

            if (scanner.isQualifier()) {
                scanner.skipSeparator()
                return VersionNumber(major, minor, micro, patch, scanner.remainder(), this)
            }

            return UNKNOWN
        }
    }

    private class Scanner(val str: String) {
        var pos = 0

        fun hasDigit(): Boolean =
            pos < str.length && str[pos].isDigit()

        fun isSeparatorAndDigit(vararg separators: Char): Boolean =
            pos < str.length - 1 && separators.contains(str[pos]) && str[pos + 1].isDigit()

        fun isQualifier(): Boolean =
            pos < str.length - 1 && charArrayOf('.', '-').contains(str[pos])

        fun scanDigit(): Int {
            val start = pos
            while (hasDigit()) { pos += 1 }
            return str.substring(start..<pos).toInt()
        }

        fun isEnd(): Boolean = pos == str.length
        fun skipSeparator() { pos += 1 }
        fun remainder(): String = str.substring(pos..<str.length)
    }
}
