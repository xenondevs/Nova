package xyz.xenondevs.nova.item

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.item.impl.FilterItem
import xyz.xenondevs.nova.util.novaMaterial

object ItemManager : Listener {
    
    private val items = ArrayList<NovaItem>()
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
        
        items.add(FilterItem)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleInteract(event: PlayerInteractEvent) {
        val material = event.item?.novaMaterial
        if (material != null) {
            items
                .filter { it.material == material }
                .forEach { it.handleInteract(event.player, event.item!!, event.action, event) }
        }
    }
    
}