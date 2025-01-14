package com.nishtahir

import java.io.File

val systemDefaultAndroidSdkHome = run {
    val os = System.getProperty("os.name").lowercase()
    val home = System.getProperty("user.home")

    if (os.contains("win")) {
        val localappdata = System.getenv("LOCALAPPDATA")
        File("$localappdata\\Android\\Sdk")
    } else if (os.contains("osx")) {
        File("$home/Library/Android/sdk")
    } else {
        File("$home/Android/sdk")
    }
}

class SimpleAndroidApp(
    private val projectDir: File,
    private val androidVersion: VersionNumber = Versions.latestAndroidVersion(),
    private val ndkVersionOverride: VersionNumber? = null,
    private val kotlinVersion: VersionNumber? = VersionNumber.parse("1.3.72"),
    private val kaptWorkersEnabled: Boolean = true
) {
    private val ndkVersion = ndkVersionOverride
        ?: if (androidVersion >= android("3.4.0")) {
            VersionNumber.parse("26.3.11579264")
        } else {
            null
        }

    private val cacheDir = File(projectDir, ".cache").apply { mkdirs() }

    fun writeProject() {
        val localRepo = System.getProperty("local.repo")

        writeFile("settings.gradle.kts", /*language=kotlin*/ """
            buildCache {
                local {
                    directory = "${cacheDir.absolutePath.replace(File.separatorChar, '/')}"
                }
            }
            
            include(":app")
            include(":library")
        """.trimIndent())

        writeFile("build.gradle.kts", /*language=kotlin*/ """
            buildscript {
                repositories {
                    google()
                    mavenCentral()
                    maven("$localRepo")
                }
                dependencies {
                    classpath("com.android.tools.build:gradle:${androidVersion}!!")
                    classpath("org.mozilla.rust-android-gradle:plugin:${Versions.PLUGIN_VERSION}")
                }
            }
        """.trimIndent())

        val libPackage = /*language=*/ "org.gradle.android.example.library"
        val libActivity = /*language=*/ "LibraryActivity"

        writeActivity("library", libPackage, libActivity)

        writeFile("library/src/main/AndroidManifest.xml", /*language=xml*/ """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$libPackage">
            </manifest>
        """.trimIndent())

        val appPackage = /*language=*/ "org.gradle.android.example.app"
        val appActivity = /*language=*/ "AppActivity"

        writeActivity("app", appPackage, appActivity)

        writeFile("app/src/main/AndroidManifest.xml", /*language=xml*/ """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$appPackage">
            
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
                        android:name="${libPackage}.${libActivity}"
                        android:exported="false" >
                    </activity>
                </application>
            
            </manifest>
        """.trimIndent())

        writeFile("app/src/main/res/values/strings.xml", /*language=xml*/ """
            <resources>
                <string name="app_name">Android Gradle</string>
            </resources>
        """.trimIndent())

        writeFile("app/build.gradle.kts",
            subprojectConfiguration("com.android.application"),
            """android.defaultConfig.applicationId = "org.gradle.android.test.app"""",
            """
                dependencies {
                    implementation(project(":library"))
                }
            """.trimIndent()
        )

        writeFile("library/build.gradle.kts", subprojectConfiguration("com.android.library"))

        writeFile("gradle.properties", /*language=properties*/ """
            android.useAndroidX=true
            org.gradle.jvmargs=-Xmx2048m
            kapt.use.worker.api=${kaptWorkersEnabled}
        """.trimIndent())

        if (System.getenv("ANDROID_HOME").isNullOrEmpty()) {
            writeFile("local.properties", /*language=properties*/ """
                sdk.dir=${systemDefaultAndroidSdkHome.absolutePath.replace(File.separatorChar, '/')}
            """.trimIndent())
        }
    }

    private fun subprojectConfiguration(androidPlugin: String) = /*language=kotlin*/ """
        apply(plugin = "$androidPlugin")
        apply(plugin = "org.mozilla.rust-android-gradle.rust-android")
        ${includeIfKotlin(/*language=*/ """
            apply(plugin = "kotlin-android")
            apply(plugin = "kotlin-kapt")
        """.trimIndent())}
        
        repositories {
            google()
            mavenCentral()
        }
        
        dependencies {
            implementation("joda-time:joda-time:2.7")
            ${includeIfKotlin("""
                implementation("org.jetbrains.kotlin:kotlin-stdlib")
            """.trimIndent())}
        }
        
        android {
            namespace = "com.nishtahir"
            ${ndkVersion?.let { """ndkVersion = "$it"""" } ?: ""}
            compileSdk = 28
            buildToolsVersion = "29.0.3"
            defaultConfig {
                minSdk = 28
                targetSdk = 28
            }
        }
    """.trimIndent()

    private fun includeIfKotlin(inclusion: String): String =
        if (kotlinVersion != null) {
            inclusion
        } else {
            ""
        }

    private fun writeActivity(baseDir: String, packageName: String, className: String) {
        val resourceName = className.lowercase()
        val packagePath = packageName.replace('.', '/')

        writeFile("${baseDir}/src/main/java/${packagePath}/${className}.java", /*language=java*/ """
            package ${packageName};
            
            import org.joda.time.LocalTime;
            
            import android.app.Activity;
            import android.os.Bundle;
            import android.widget.TextView;
            
            public class $className extends Activity {
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
        """.trimIndent())

        writeFile("${baseDir}/src/test/java/${packagePath}/JavaUserTest.java", /*language=java*/ """
            package ${packageName};
            
            public class JavaUserTest {}
        """.trimIndent())

        writeFile("${baseDir}/src/main/res/layout/${resourceName}_layout.xml", /*language=xml*/ """
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
        """.trimIndent())

        writeFile("${baseDir}/src/main/rs/${resourceName}.rs", /*language=renderscript*/ """
            #pragma version(1)
            #pragma rs java_package_name(com.example.myapplication)
            
            static void addintAccum(int *accum, int val) {
                *accum += val;
            }
        """.trimIndent())
    }

    private fun writeFile(relativePath: String, vararg contents: String) {
        File(projectDir, relativePath).apply {
            parentFile.mkdirs()
            contents.forEach { appendText(it + '\n') }
        }
    }
}