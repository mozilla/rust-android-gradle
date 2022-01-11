package com.nishtahir

class SimpleCargoProject {
    File projectDir
    List<String> targets

    SimpleCargoProject(File projectDir, List<String> targets) {
        this.projectDir = projectDir
        this.targets = targets
    }

    static class Builder {
        File projectDir
        List<String> targets

        Builder(File projectDir) {
            this.projectDir = projectDir
        }

        def withTargets(targets) {
            this.targets = targets
            return this
        }

        def build() {
            if (targets.isEmpty()) {
                throw new IllegalStateException("No targets provided")
            }
            return new SimpleCargoProject(this.projectDir, this.targets)
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
        def cargoModule = this.class.classLoader.getResource("rust/Cargo.toml").path
        cargoModule = new File(cargoModule).parent

        def targetStrings = targets.collect({"\"${it}\"" }).join(", ")

        file('app/build.gradle') << """
                cargo {
                    module = "${cargoModule}"
                    targetDirectory = "${cargoModule}/../target"
                    targets = [${targetStrings}]
                    libname = "rust"
                }
            """.stripIndent()

        file('library/build.gradle') << """
                cargo {
                    module = "${cargoModule}"
                    targetDirectory = "${cargoModule}/../target"
                    targets = [${targetStrings}]
                    libname = "rust"
                }
            """.stripIndent()
    }
}
