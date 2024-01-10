package com.nishtahir

class SimpleCargoProject {
    File projectDir
    List<String> targets
    String channel

    SimpleCargoProject(File projectDir, List<String> targets, String channel) {
        this.projectDir = projectDir
        this.targets = targets
        this.channel = channel
    }

    static class Builder {
        File projectDir
        List<String> targets
        String channel

        Builder(File projectDir) {
            this.projectDir = projectDir
        }

        def withTargets(targets) {
            this.targets = targets
            return this
        }

        def withChannel(channel) {
            this.channel = channel
            return this
        }

        def selectChannelFromNdkVersion(ndkVersion) {
            def ndkVersionMajor = ndkVersion.split('\\.')[0] as int
            // Toolchain 1.68 or later versions are not compatible to old NDK prior to r23
            // https://blog.rust-lang.org/2023/01/09/android-ndk-update-r25.html
            this.channel = ndkVersionMajor >= 23 ? "stable" : "1.67"
            return this

        }

        def build() {
            if (targets.isEmpty()) {
                throw new IllegalStateException("No targets provided")
            }
            return new SimpleCargoProject(this.projectDir, this.targets, this.channel)
        }
    }

    static def builder(File projectDir) {
        return new Builder(projectDir)
    }

    def file(String path) {
        def file = new File(projectDir, path)
        file.parentFile.mkdirs()
        return file
    }

    def writeProject() {
        def cargoModuleFile = new File(this.class.classLoader.getResource("rust/Cargo.toml").path).parentFile
        def targetDirectoryFile = new File(cargoModuleFile.parentFile, "target")

        // On Windows, path components are backslash-separated.  We need to
        // express the path as Groovy source, which means backslashes need to be
        // escaped.  The easiest way is to replace backslashes with forward
        // slashes.
        def module = cargoModuleFile.path.replace("\\", "/")
        def targetDirectory = targetDirectoryFile.path.replace("\\", "/")

        def targetStrings = targets.collect({"\"${it}\"" }).join(", ")

        file('app/build.gradle') << """
                cargo {
                    module = "${module}"
                    targetDirectory = "${targetDirectory}"
                    targets = [${targetStrings}]
                    libname = "rust"
                }
            """.stripIndent()

        file('library/build.gradle') << """
                cargo {
                    module = "${module}"
                    targetDirectory = "${targetDirectory}"
                    targets = [${targetStrings}]
                    libname = "rust"
                }
            """.stripIndent()

        if (channel != null) {
            file('local.properties') << """
                rust.rustupChannel=${channel}
            """.stripIndent()
        }
    }
}
