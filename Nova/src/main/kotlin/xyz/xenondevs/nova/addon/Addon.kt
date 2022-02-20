package xyz.xenondevs.nova.addon

import java.io.File
import java.util.logging.Logger

abstract class Addon {
    
    lateinit var logger: Logger
    lateinit var addonFile: File
    lateinit var dataFolder: File
    lateinit var description: AddonDescription
    
    abstract fun init()
    abstract fun onEnable()
    abstract fun onDisable()
    
}