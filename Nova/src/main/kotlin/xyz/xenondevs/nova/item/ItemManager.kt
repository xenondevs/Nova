package xyz.xenondevs.nova.item

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.network.event.serverbound.PlayerActionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.UseItemPacketEvent
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.bukkitSlot
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.takeUnlessAir

internal object ItemManager : Initializable(), Listener {
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    private val usedItems = HashMap<Player, ItemStack>()
    
    override fun init() {
        LOGGER.info("Initializing ItemManager")
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler(priority = EventPriority.LOW)
    private fun handleInteract(e: WrappedPlayerInteractEvent) {
        val event = e.event
        if (event.isCompletelyDenied()) return
        
        findBehaviors(event.item)?.forEach { it.handleInteract(event.player, event.item!!, event.action, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val item = event.player.inventory.getItem(event.hand)
        findBehaviors(item)?.forEach { it.handleEntityInteract(event.player, item!!, event.rightClicked, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun handleBreak(event: PlayerItemBreakEvent) {
        findBehaviors(event.brokenItem)?.forEach { it.handleBreak(event.player, event.brokenItem, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previousArmorItem
        val equippedItem = event.newArmorItem
        
        findBehaviors(unequippedItem)?.forEach { it.handleEquip(player, unequippedItem!!, false, event) }
        findBehaviors(equippedItem)?.forEach { it.handleEquip(player, equippedItem!!, true, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleInteract(event: InventoryClickEvent) {
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
    
    // This method stores the last used item for the RELEASE_USE_ITEM action below
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleUseItem(event: UseItemPacketEvent) {
        val player = event.player
        val item = player.inventory.getItem(event.hand.bukkitSlot)?.takeUnlessAir()
        if (item != null)
            usedItems[player] = item
        else usedItems -= player
    }
    
    @EventHandler
    private fun handlePlayerQuit(event: PlayerQuitEvent) {
        usedItems -= event.player
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleAction(event: PlayerActionPacketEvent) {
        if (event.action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            val player = event.player
            val item = usedItems[player]
            findBehaviors(item)?.forEach { it.handleRelease(player, item!!, event) }
        }
    }
    
    private fun findBehaviors(item: ItemStack?): List<ItemBehavior>? =
        item?.novaMaterial?.novaItem?.behaviors
    
}

