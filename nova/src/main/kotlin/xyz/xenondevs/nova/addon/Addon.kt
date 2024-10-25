@file:Suppress("LeakingThis", "UnstableApiUsage")

package xyz.xenondevs.nova.addon

import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.update.ProjectDistributor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.io.File

private val JAVA_PLUGIN_GET_FILE = ReflectionUtils.getMethodHandle(JavaPlugin::class, "getFile")

@PublishedApi
internal val Addon.name: String
    get() = (this as JavaPlugin).pluginMeta.name

@PublishedApi
internal val Addon.id: String
    get() = (this as JavaPlugin).pluginMeta.name.lowercase()

@PublishedApi
internal val Addon.version: String
    get() = (this as JavaPlugin).pluginMeta.version

@PublishedApi
internal val Addon.file: File
    get() = JAVA_PLUGIN_GET_FILE.invoke(this as JavaPlugin) as File

interface Addon : AddonHolder {
    
    /**
     * A list of [ProjectDistributors][ProjectDistributor] that distribute this addon
     * and should be checked for updates.
     */
    val projectDistributors: List<ProjectDistributor>
        get() = emptyList()
    
    override val addon: Addon
        get() = this
    
}