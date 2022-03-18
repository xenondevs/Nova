package xyz.xenondevs.nova.item

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.novaMaterial

object ItemManager : Initializable(), Listener {
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Initializing ItemManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.isCompletelyDenied()) return
        findBehaviors(event.item)?.forEach { it.handleInteract(event.player, event.item!!, event.action, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        if (event.isCancelled) return
        val item = event.player.inventory.getItem(event.hand)
        findBehaviors(item)?.forEach { it.handleEntityInteract(event.player, item!!, event.rightClicked, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handleBreak(event: PlayerItemBreakEvent) {
        findBehaviors(event.brokenItem)?.forEach { it.handleBreak(event.player, event.brokenItem, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previousArmorItem
        val equippedItem = event.newArmorItem
        
        findBehaviors(unequippedItem)?.forEach { it.handleEquip(player, unequippedItem!!, false, event) }
        findBehaviors(equippedItem)?.forEach { it.handleEquip(player, equippedItem!!, true, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedItem = event.currentItem
        val cursorItem = event.cursor
        
        findBehaviors(clickedItem)?.forEach { it.handleInventoryClick(player, clickedItem!!, event) }
        findBehaviors(cursorItem)?.forEach { it.handleInventoryClickOnCursor(player, cursorItem!!, event) }
        
        if (event.click == ClickType.NUMBER_KEY) {
            val hotbarItem = player.inventory.getItem(event.hotbarButton)
            findBehaviors(hotbarItem)?.forEach { it.handleInventoryHotbarSwap(player, hotbarItem!!, event) }
        }
    }
    
    private fun findBehaviors(item: ItemStack?): List<ItemBehavior>? =
        item?.novaMaterial?.novaItem?.behaviors
    
}

