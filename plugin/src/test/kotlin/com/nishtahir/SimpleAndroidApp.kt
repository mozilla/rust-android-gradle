package com.nishtahir

import java.io.File

class SimpleAndroidApp(
    private val projectDir: File,
    private val androidVersion: VersionNumber = Versions.latestAndroidVersion(),
    private val ndkVersionOverride: VersionNumber? = null,
    private val kotlinVersion: VersionNumber,
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

        appendFile("settings.gradle.kts", /*language=kotlin*/ """
            pluginManagement {
                repositories {
                    maven("$localRepo")
                    google()
                    mavenCentral()
                    gradlePluginPortal {
                        content {
                            excludeGroup("me.sigptr.rust-android")
                        }
                    }
                }
            }
            
            buildCache {
                local {
                    directory = "${cacheDir.absolutePath.replace(File.separatorChar, '/')}"
                }
            }
            
            include(":app")
            include(":library")
        """.trimIndent())

        val libPackage =/*language=*/ "org.gradle.android.example.library"
        val libActivity =/*language=*/ "LibraryActivity"

        writeActivity("library", libPackage, libActivity)

        appendFile("library/src/main/AndroidManifest.xml", /*language=xml*/ """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="$libPackage">
            </manifest>
        """.trimIndent())

        val appPackage =/*language=*/ "org.gradle.android.example.app"
        val appActivity =/*language=*/ "AppActivity"

        writeActivity("app", appPackage, appActivity)

        appendFile("app/src/main/AndroidManifest.xml", /*language=xml*/ """
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

        appendFile("app/src/main/res/values/strings.xml", /*language=xml*/ """
            <resources>
                <string name="app_name">Android Gradle</string>
            </resources>
        """.trimIndent())

        appendFile("app/build.gradle.kts",
            subprojectConfiguration("com.android.application"),
            """android.defaultConfig.applicationId = "org.gradle.android.test.app"""",
            """
                dependencies {
                    implementation(project(":library"))
                }
            """.trimIndent()
        )

        appendFile("library/build.gradle.kts", subprojectConfiguration("com.android.library"))

        appendFile("gradle.properties", /*language=properties*/ """
            android.useAndroidX=true
            org.gradle.jvmargs=-Xmx2048m
            kapt.use.worker.api=${kaptWorkersEnabled}
        """.trimIndent())
    }

    private fun subprojectConfiguration(androidPlugin: String) = /*language=kotlin*/ """
        plugins {
            id("org.jetbrains.kotlin.android") version("$kotlinVersion")
            id("$androidPlugin") version("$androidVersion")
            id("me.sigptr.rust-android") version("${Versions.PLUGIN_VERSION}")
        }
        
        repositories {
            google()
            mavenCentral()
        }
        
        dependencies {
            implementation("joda-time:joda-time:2.7")
        }
        
        android {
            namespace = "com.nishtahir"
            ${ndkVersion?.let { """ndkVersion = "$it"""" } ?: ""}
            compileSdk = 28
            defaultConfig {
                minSdk = 28
            }
        }
    """.trimIndent()

    private fun writeActivity(baseDir: String, packageName: String, className: String) {
        val resourceName = className.lowercase()
        val packagePath = packageName.replace('.', '/')

        appendFile("${baseDir}/src/main/java/${packagePath}/${className}.java", /*language=java*/ """
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

        appendFile("${baseDir}/src/test/java/${packagePath}/JavaUserTest.java", /*language=java*/ """
            package ${packageName};
            
            public class JavaUserTest {}
        """.trimIndent())

        appendFile("${baseDir}/src/main/res/layout/${resourceName}_layout.xml", /*language=xml*/ """
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

        appendFile("${baseDir}/src/main/rs/${resourceName}.rs", /*language=renderscript*/ """
            #pragma version(1)
            #pragma rs java_package_name(com.example.myapplication)
            
            static void addintAccum(int *accum, int val) {
                *accum += val;
            }
        """.trimIndent())
    }

    private fun appendFile(relativePath: String, vararg contents: String) {
        File(projectDir, relativePath).apply {
            parentFile.mkdirs()
            contents.forEach {
                appendText(it)
                appendText("\n\n")
            }
        }
    }
}