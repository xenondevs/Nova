package xyz.xenondevs.nova.item

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundUseItemPacketEvent
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.bukkitSlot
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener

internal object ItemListener : Initializable(), Listener {
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = emptySet<Initializable>()
    
    private val usedItems = HashMap<Player, ItemStack>()
    
    override fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    private fun handleInteract(e: WrappedPlayerInteractEvent) {
        val event = e.event
        val player = event.player
        val item = event.item
        
        val location = event.clickedBlock?.location ?: player.location
        if (event.isCompletelyDenied() || item == null || !ProtectionManager.canUseItem(player, item, location).get())
            return
        
        findBehaviors(event.item)?.forEach { it.handleInteract(event.player, event.item!!, event.action, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val item = event.player.inventory.getItem(event.hand)
        findBehaviors(item)?.forEach { it.handleEntityInteract(event.player, item!!, event.rightClicked, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val item = player.inventory.getItem(EquipmentSlot.HAND)
        findBehaviors(item)?.forEach { it.handleAttackEntity(player, item!!, event.entity, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockBreak(event: BlockBreakEvent) {
        val item = event.player.inventory.getItem(EquipmentSlot.HAND)
        findBehaviors(item)?.forEach { it.handleBreakBlock(event.player, item!!, event) }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleDamage(event: PlayerItemDamageEvent) {
        val item = event.item
        findBehaviors(item)?.forEach { it.handleDamage(event.player, item, event) }
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
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
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
    @PacketHandler(priority = EventPriority.HIGHEST, ignoreIfCancelled = true)
    private fun handleUseItem(event: ServerboundUseItemPacketEvent) {
        val player = event.player
        val item = player.inventory.getItem(event.hand.bukkitSlot)?.takeUnlessEmpty()
        if (item != null)
            usedItems[player] = item
        else usedItems -= player
    }
    
    @EventHandler
    private fun handlePlayerQuit(event: PlayerQuitEvent) {
        usedItems -= event.player
    }
    
    @PacketHandler(priority = EventPriority.HIGHEST, ignoreIfCancelled = true)
    private fun handleAction(event: ServerboundPlayerActionPacketEvent) {
        if (event.action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            val player = event.player
            val item = usedItems[player]
            findBehaviors(item)?.forEach { it.handleRelease(player, item!!, event) }
        }
    }
    
    private fun findBehaviors(item: ItemStack?): List<ItemBehavior>? =
        item?.novaMaterial?.novaItem?.behaviors
    
}

