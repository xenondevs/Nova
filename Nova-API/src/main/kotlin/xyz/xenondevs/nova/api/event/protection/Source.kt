package xyz.xenondevs.nova.api.event.protection

import org.bukkit.OfflinePlayer

open class Source(val player: OfflinePlayer) {
    
    override fun toString(): String {
        return "Source(player=$player)"
    }
    
}