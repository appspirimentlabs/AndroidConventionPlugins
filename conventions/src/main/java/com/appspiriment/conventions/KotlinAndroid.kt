package com.appspiriment.conventions

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    version: ModuleVersionInfo
) : ProjectVersions{
    val projectConfig = configs
    commonExtension.apply {
        compileSdk = projectConfig.compileSdk

        defaultConfig.apply {
            minSdk = projectConfig.minSdk
            buildConfigField("int", "VERSIONCODE", "${version.releaseCode}")

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }

            compileOptions {
                sourceCompatibility = projectConfig.javaVersion
                targetCompatibility = projectConfig.javaVersion
            }

            val proguardFile = "proguard-rules.pro"
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        proguardFile
                    )
                }
                getByName("debug") {
                    isMinifyEnabled = false
                }
            }
        }

        project.tasks.withType(KotlinCompile::class.java).configureEach {
            kotlinOptions {
                jvmTarget = projectConfig.javaVersion.toString()
            }
        }

        buildFeatures {
            buildConfig = true
        }
        //configureFlavors(this)
        //configureGradleManagedDevices(this)
        packaging {
            resources {
                resources.excludes.add("META-INF/*")
            }
        }

        compileOptions {

            sourceCompatibility = projectConfig.javaVersion
            targetCompatibility = projectConfig.javaVersion
            isCoreLibraryDesugaringEnabled = false
        }

    }


    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            // Set JVM target to 11
            jvmTarget = projectConfig.javaVersion.toString()
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors = warningsAsErrors.toBoolean()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                // Enable experimental coroutines APIs, including Flow
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
        }
    }

    return projectConfig
}

internal fun getVersionCodes(): ModuleVersionInfo {
    val propsFile = File("version.properties").apply {
        if (!exists()) writeText(
            "MAJOR=0\nPATCH=0\nREVISION=0\nRELEASE_REVISION=0"
        )
    }

    if (propsFile.exists()) {
        val props = Properties()
        props.load(FileInputStream(propsFile))
        val major = props["MAJOR"].toString().padStart(2, '0')
        var patch = props["PATCH"].toString().padStart(2, '0')
        val revisionStr = props["REVISION"].toString()
        val releaseRevisionStr = props["RELEASE_REVISION"].toString()
        val revision = revisionStr.toInt().let {
            if (it >= 9999) 0.also {
                patch = "${patch.toInt() + 1}"
            } else it
        }.toString().padStart(revisionStr.length, '0')

        val verCode = "$major$patch$revision".toInt()
        val releaseVerCode = "$major$patch$releaseRevisionStr".toInt()
        val (debugVerName, releaseVerName) = "$major.$patch".let {
            Pair("$it.$revision", "$it.0")
        }

        return ModuleVersionInfo(
            debugCode = verCode,
            debugName = debugVerName,
            releaseCode = releaseVerCode,
            releaseName = releaseVerName
        ).also {
            propsFile.writeText("MAJOR=$major\nPATCH=$patch\nREVISION=${revision.toInt() + 1}\nRELEASE_REVISION=$releaseRevisionStr")
        }
    } else {
        throw FileNotFoundException("Could not read version.properties!")
    }
}

data class ModuleVersionInfo(
    val debugCode: Int,
    val debugName: String,
    val releaseCode: Int,
    val releaseName: String
)