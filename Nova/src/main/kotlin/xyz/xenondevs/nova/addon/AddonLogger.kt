package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import java.util.logging.Level
import java.util.logging.Logger

open class AddonLogger(addon: Addon) : Logger(addon.description.name, null) {
    
    init {
        parent = LOGGER
        level = Level.ALL
    }
    
}