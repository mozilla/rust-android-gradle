package com.nishtahir

import org.gradle.util.VersionNumber

import static com.nishtahir.Versions.android

class SimpleAndroidApp {
    final File projectDir
    private final File cacheDir
    final VersionNumber androidVersion
    final VersionNumber ndkVersion
    final VersionNumber kotlinVersion
    private final boolean kotlinEnabled
    private final boolean kaptWorkersEnabled

    private SimpleAndroidApp(File projectDir, File cacheDir, VersionNumber androidVersion, VersionNumber ndkVersion, VersionNumber kotlinVersion, boolean kotlinEnabled, boolean kaptWorkersEnabled) {
        this.projectDir = projectDir
        this.cacheDir = cacheDir
        this.androidVersion = androidVersion
        this.ndkVersion = ndkVersion
        this.kotlinVersion = kotlinVersion
        this.kotlinEnabled = kotlinEnabled
        this.kaptWorkersEnabled = kaptWorkersEnabled
    }

    def writeProject() {
        def app = 'app'
        def appPackage = 'org.gradle.android.example.app'
        def appActivity = 'AppActivity'

        def library = 'library'
        def libPackage = 'org.gradle.android.example.library'
        def libraryActivity = 'LibraryActivity'

        file("settings.gradle") << """
                buildCache {
                    local {
                        directory = "${cacheDir.absolutePath.replace(File.separatorChar, '/' as char)}"
                    }
                }
            """.stripIndent()

        file("build.gradle") << """
                buildscript {
                    repositories {
                        google()
                        mavenCentral()
                        maven {
                            url = "${System.getProperty("local.repo")}"
                        }
                    }
                    dependencies {
                        //classpath ('com.android.tools.build:gradle:$androidVersion') { force = true }
                        classpath ('com.android.tools.build:gradle') { version { strictly "$androidVersion" } }
                        classpath "org.mozilla.rust-android-gradle:plugin:${Versions.PLUGIN_VERSION}"
                        ${kotlinPluginDependencyIfEnabled}
                    }
                }
            """.stripIndent()

        writeActivity(library, libPackage, libraryActivity)
        file("${library}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${libPackage}">
                </manifest>
            """.stripIndent()

        writeActivity(app, appPackage, appActivity)
        file("${app}/src/main/AndroidManifest.xml") << """<?xml version="1.0" encoding="utf-8"?>
                <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                    package="${appPackage}">

                    <application android:label="@string/app_name" >
                        <activity
                            android:name=".${appActivity}"
                            android:label="@string/app_name"
                            android:exported="true" >
                            <intent-filter>
                                <action android:name="android.intent.action.MAIN" />
                                <category android:name="android.intent.category.LAUNCHER" />
                            </intent-filter>
                        </activity>
                        <activity
                            android:name="${libPackage}.${libraryActivity}"
                            android:exported="false" >
                        </activity>
                    </application>

                </manifest>
            """.stripIndent()
        file("${app}/src/main/res/values/strings.xml") << '''<?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <string name="app_name">Android Gradle</string>
                </resources>'''.stripIndent()

        file('settings.gradle') << """
                include ':${app}'
                include ':${library}'
            """.stripIndent()

        file("${app}/build.gradle") << subprojectConfiguration("com.android.application", appPackage) << """
                android.defaultConfig.applicationId "org.gradle.android.test.app"
            """.stripIndent() << activityDependency() <<
            """
                dependencies {
                    implementation project(':${library}')
                }
            """.stripIndent()

        file("${library}/build.gradle") << subprojectConfiguration("com.android.library", libPackage) << activityDependency()

        file("gradle.properties") << """
                android.useAndroidX=true
                org.gradle.jvmargs=-Xmx2048m
                kapt.use.worker.api=${kaptWorkersEnabled}
            """.stripIndent()

        configureAndroidSdkHome()
    }

    private String getKotlinPluginDependencyIfEnabled() {
        return kotlinEnabled ? """
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}"
        """ : ""
    }

    private subprojectConfiguration(String androidPlugin, String namespace) {
        def maybeNamespace = androidVersion >= android("8.0.0") ? "namespace \"${namespace}\"" : ""

        """
            apply plugin: "$androidPlugin"
            ${kotlinPluginsIfEnabled}
            apply plugin: "org.mozilla.rust-android-gradle.rust-android"

            repositories {
                google()
                mavenCentral()
            }

            dependencies {
                ${kotlinDependenciesIfEnabled}
            }

            android {
                ${maybeNamespace}
                ${maybeNdkVersion}
                compileSdkVersion 28
                buildToolsVersion "29.0.3"
                defaultConfig {
                    minSdkVersion 28
                    targetSdkVersion 28

                    lintOptions {
                        checkReleaseBuilds false
                    }
                }
            }
        """.stripIndent()
    }

    private String getMaybeNdkVersion() {
        if (ndkVersion != null && androidVersion >= android("3.5.0")) {
            return """ndkVersion '${ndkVersion}'"""
        } else {
            return ""
        }
    }

    private String getKotlinPluginsIfEnabled() {
        return kotlinEnabled ? """
            apply plugin: "kotlin-android"
            apply plugin: "kotlin-kapt"
        """ : ""
    }

    private String getKotlinDependenciesIfEnabled() {
        return kotlinEnabled ? """
            implementation "org.jetbrains.kotlin:kotlin-stdlib"
        """ : ""
    }

    private writeActivity(String basedir, String packageName, String className) {
        String resourceName = className.toLowerCase()

        file("${basedir}/src/main/java/${packageName.replaceAll('\\.', '/')}/${className}.java") << """
                package ${packageName};

                import org.joda.time.LocalTime;

                import android.app.Activity;
                import android.os.Bundle;
                import android.widget.TextView;

                public class ${className} extends Activity {

                    @Override
                    public void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.${resourceName}_layout);
                    }

                    @Override
                    public void onStart() {
                        super.onStart();
                        LocalTime currentTime = new LocalTime();
                        TextView textView = (TextView) findViewById(R.id.text_view);
                        textView.setText("The current local time is: " + currentTime);
                    }
                }
            """.stripIndent()

        file("${basedir}/src/test/java/${packageName.replaceAll('\\.', '/')}/JavaUserTest.java") << """
                package ${packageName};

                public class JavaUserTest {
                }
            """.stripIndent()

        file("${basedir}/src/main/res/layout/${resourceName}_layout.xml") << '''<?xml version="1.0" encoding="utf-8"?>
                <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                    android:orientation="vertical"
                    android:layout_width="fill_parent"
                    android:layout_height="fill_parent"
                    >
                <TextView
                    android:id="@+id/text_view"
                    android:layout_width="fill_parent"
                    android:layout_height="wrap_content"
                    />
                </LinearLayout>
            '''.stripIndent()

        file("${basedir}/src/main/rs/${resourceName}.rs") << '''
                #pragma version(1)
                #pragma rs java_package_name(com.example.myapplication)

                static void addintAccum(int *accum, int val) {
                  *accum += val;
                }
            '''.stripIndent()
    }

    private static String activityDependency() {
        """
            dependencies {
                implementation 'joda-time:joda-time:2.7'
            }
        """.stripIndent()
    }

    private void configureAndroidSdkHome() {
        def sdkDir = getSdkDirectory()?.replace("\\", "\\\\")
        def ndkDir = getNdkDirectory()?.replace("\\", "\\\\")
        def maybeNdkDir = ndkDir != null ? "ndk.dir=$ndkDir" : ""
        println("SDK directory: $sdkDir")
        println("NDK directory: $ndkDir")
        file('local.properties') << """
            sdk.dir=$sdkDir
            ${maybeNdkDir}
        """.stripIndent()
    }

    // Get SDK directory from local.properties and environment variables
    private static String getSdkDirectory() {
        // get root project directory
        def rootProjectDir = System.getProperty("local.root_project.dir")
        def sdkDir = null

        // get sdk directory from local.properties in root project
        def localProperties = new File(rootProjectDir, "local.properties")
        if (localProperties.exists()) {
            Properties properties = new Properties()
            localProperties.withInputStream { properties.load(it) }
            sdkDir = properties.getProperty("sdk.dir")
        }

        // get sdk directory from environment variables
        if (sdkDir == null) {
            sdkDir = System.getenv("ANDROID_HOME")
        }

        // Fallback in MacOS
        if (sdkDir == null) {
            def androidSdkHome = new File(System.getProperty("user.home"), "Library/Android/sdk")
            sdkDir = androidSdkHome.absolutePath.replace(File.separatorChar, '/' as char)
        }

        return sdkDir
    }

    private String getNdkDirectory() {
        if (androidVersion >= android("3.5.0")) {
            return null
        }

        if (sdkDirectory == null || ndkVersion == null) {
            return null
        }

        return new File(sdkDirectory, "ndk/${ndkVersion}").absolutePath
    }

    def file(String path) {
        def file = new File(projectDir, path)
        file.parentFile.mkdirs()
        return file
    }

    static Builder builder(File projectDir, File cacheDir) {
        return new Builder(projectDir, cacheDir)
    }

    static class Builder {
        boolean kotlinEnabled = true
        boolean kaptWorkersEnabled = true

        VersionNumber androidVersion = Versions.latestAndroidVersion()
        VersionNumber ndkVersion = VersionNumber.parse("25.2.9519653")

        VersionNumber kotlinVersion = VersionNumber.parse("1.3.72")
        File projectDir
        File cacheDir

        Builder(File projectDir, File cacheDir) {
            this.projectDir = projectDir
            this.cacheDir = cacheDir
        }

        Builder withKotlinDisabled() {
            this.kotlinEnabled = false
            return this
        }

        Builder withKotlinVersion(VersionNumber kotlinVersion) {
            this.kotlinVersion = kotlinVersion
            return this
        }

        Builder withKaptWorkersDisabled() {
            this.kaptWorkersEnabled = false
            return this
        }

        Builder withAndroidVersion(VersionNumber androidVersion) {
            this.androidVersion = androidVersion
            return this
        }

        Builder withAndroidVersion(String androidVersion) {
            return withAndroidVersion(android(androidVersion))
        }

        Builder withNdkVersion(VersionNumber ndkVersion) {
            this.ndkVersion = ndkVersion
            return this
        }

        Builder withNdkVersion(String ndkVersion) {
            return withNdkVersion(VersionNumber.parse(ndkVersion))
        }

        Builder withProjectDir(File projectDir) {
            this.projectDir = projectDir
            return this
        }

        Builder withCacheDir(File cacheDir) {
            this.cacheDir = cacheDir
            return this
        }

        SimpleAndroidApp build() {
            return new SimpleAndroidApp(projectDir, cacheDir, androidVersion, ndkVersion, kotlinVersion, kotlinEnabled, kaptWorkersEnabled)
        }
    }
}
