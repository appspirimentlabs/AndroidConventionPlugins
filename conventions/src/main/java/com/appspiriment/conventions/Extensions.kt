package com.appspiriment.conventions

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project
import kotlin.jvm.optionals.getOrElse

internal fun PluginManager.applyPluginFromLibs(
    vararg pluginIdList: Pair<VersionCatalog, List<String>>
) {
    pluginIdList.forEach { group ->
        group.second.forEach {
            group.first.findPlugin(it).ifPresentOrElse({ plugin ->
                apply(plugin.get().pluginId)
            }) {
                throw Exception("Plugin $it not found in ${group.first.name}")
            }
        }
    }
}

internal fun DependencyHandlerScope.implementDependency(
    libs: VersionCatalog,
    dependencyList: List<Dependency>
) {
    dependencyList.forEach {
        when (it.type) {
            ImplType.BUDNDLE -> {
                implement(libs, it.config, it.aliases, isBundle = true)
            }

            ImplType.DEPENDENCY -> {
                implement(libs, it.config, it.aliases)
            }

            ImplType.PROJECT -> {
                it.aliases.forEach { alias ->
                    add(it.config, project(alias))
                }
            }

            ImplType.PLATFORM -> {
                implement(libs, it.config, it.aliases, isPlatform = true)
            }
        }
    }
}

internal fun DependencyHandlerScope.implement(
    libs: VersionCatalog,
    config: String,
    aliases: List<String>,
    isBundle: Boolean = false,
    isPlatform: Boolean = false,
) {
    aliases.forEach { alias ->
        libs.run {
            if (isBundle) {
                findBundle(alias).getOrElse {
                    throw Exception("Dependency Bundle $alias not found in ${libs.name}")
                }?.let {
                    add(config, it)
                }
            } else {
                findLibrary(alias).getOrElse {
                    throw Exception("Dependency $alias not found in ${libs.name}")
                }?.let {
                    add(config, if (isPlatform) platform(it) else it)
                }
            }
        }
    }
}

data class Dependency(
    val type: ImplType = ImplType.DEPENDENCY,
    val config: String,
    val aliases: List<String>,
)

enum class ImplType {
    BUDNDLE, DEPENDENCY, PROJECT, PLATFORM
}


open class AppspirimentExtension(
    var pluginList: List<String> = emptyList(),
    var dependencyList: List<Dependency> = emptyList()
)