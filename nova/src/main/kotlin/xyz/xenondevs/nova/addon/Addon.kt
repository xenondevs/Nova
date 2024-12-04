@file:Suppress("LeakingThis", "UnstableApiUsage")

package xyz.xenondevs.nova.addon

import io.papermc.paper.plugin.configuration.PluginMeta
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.addon.registry.AddonRegistryHolder
import xyz.xenondevs.nova.update.ProjectDistributor
import java.nio.file.Path

@PublishedApi
internal val Addon.name: String
    get() = pluginMeta.name

@PublishedApi
internal val Addon.id: String
    get() = pluginMeta.name.lowercase()

@PublishedApi
internal val Addon.version: String
    get() = pluginMeta.version

abstract class Addon : AddonGetter {
    
    final override val addon: Addon
        get() = this
    
    val registry = AddonRegistryHolder(this)
    
    /**
     * A list of [ProjectDistributors][ProjectDistributor] that distribute this addon
     * and should be checked for updates.
     */
    open val projectDistributors: List<ProjectDistributor>
        get() = emptyList()
    
    /**
     * The [JavaPlugin] instance of this addon, null during bootstrap phase.
     */
    var plugin: JavaPlugin? = null
        internal set
    
    /**
     * The [PluginMeta] of this addon.
     */
    lateinit var pluginMeta: PluginMeta
        internal set
    
    /**
     * The [Path] of the file of this addon.
     */
    lateinit var file: Path
        internal set
    
    /**
     * The [Path] of the data folder of this addon.
     */
    lateinit var dataFolder: Path
        internal set
    
    /**
     * The [ComponentLogger] of this addon.
     */
    lateinit var logger: ComponentLogger
        internal set
    
}