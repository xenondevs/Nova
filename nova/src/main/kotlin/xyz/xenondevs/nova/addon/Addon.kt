@file:Suppress("LeakingThis")

package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.update.ProjectDistributor
import java.io.File
import java.util.logging.Logger

abstract class Addon {
    
    lateinit var logger: Logger internal set
    lateinit var addonFile: File internal set
    lateinit var dataFolder: File internal set
    lateinit var description: AddonDescription internal set
    
    /**
     * A list of [ProjectDistributors][ProjectDistributor] that distribute this addon.
     * 
     * This list is used to check for updates.
     */
    open val projectDistributors: List<ProjectDistributor> = emptyList()
    
    val registry = AddonRegistryHolder(this)
    
    open fun init() = Unit
    open fun onEnable() = Unit
    open fun onDisable() = Unit
    
}