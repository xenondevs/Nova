package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.data.resources.builder.ResourceFilter
import java.io.File
import java.util.logging.Logger

abstract class Addon {
    
    lateinit var logger: Logger
    lateinit var addonFile: File
    lateinit var dataFolder: File
    lateinit var description: AddonDescription
    var resourceFilter: ResourceFilter? = null
    
    open fun init() = Unit
    open fun onEnable() = Unit
    open fun onDisable() = Unit
    
}