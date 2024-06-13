package xyz.xenondevs.nmsutils

import org.bukkit.plugin.Plugin
import xyz.xenondevs.nmsutils.network.PacketManager

internal lateinit var PLUGIN: Plugin
    private set

internal val LOGGER by lazy { PLUGIN.logger }

object NMSUtilities {
    
    fun init(plugin: Plugin) {
        PLUGIN = plugin
        PacketManager.init()
    }
    
    fun disable() {
        PacketManager.disable()
    }
    
}