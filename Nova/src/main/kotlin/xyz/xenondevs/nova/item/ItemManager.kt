package xyz.xenondevs.nova.item

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.novaMaterial

object ItemManager : Initializable(), Listener {
    override val inMainThread = false
    override val dependsOn = CustomItemServiceManager
    
    override fun init() {
        LOGGER.info("Initializing ItemManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.isCompletelyDenied()) return
        event.item?.novaMaterial?.novaItem?.handleInteract(event.player, event.item!!, event.action, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        if (event.isCancelled) return
        val item = event.player.inventory.getItem(event.hand)
        if (item == null || item.type == Material.AIR) return
        item.novaMaterial?.novaItem?.handleEntityInteract(event.player, item, event.rightClicked, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handleBreak(event: PlayerItemBreakEvent) {
        event.brokenItem.novaMaterial?.novaItem?.handleBreak(event.player, event.brokenItem, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previousArmorItem
        val equippedItem = event.newArmorItem
        unequippedItem?.novaMaterial?.novaItem?.handleEquip(player, unequippedItem, false, event)
        equippedItem?.novaMaterial?.novaItem?.handleEquip(player, equippedItem, true, event)
    }
    
}