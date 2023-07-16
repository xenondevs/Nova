package xyz.xenondevs.nova.item.logic

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
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundUseItemPacketEvent
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import java.util.*

@InternalInit(stage = InternalInitStage.POST_WORLD_ASYNC)
internal object ItemListener : Listener {
    
    private val usedItems = WeakHashMap<Player, ItemStack>()
    
    @InitFun
    private fun init() {
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
        
        item.logic?.handleInteract(event.player, item, event.action, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val item = event.player.inventory.getItem(event.hand).takeUnlessEmpty()
        item?.logic?.handleEntityInteract(event.player, item, event.rightClicked, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val item = player.inventory.getItem(EquipmentSlot.HAND).takeUnlessEmpty()
        item?.logic?.handleAttackEntity(player, item, event.entity, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockBreak(event: BlockBreakEvent) {
        val item = event.player.inventory.getItem(EquipmentSlot.HAND).takeUnlessEmpty()
        item?.logic?.handleBreakBlock(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleDamage(event: PlayerItemDamageEvent) {
        val item = event.item
        item.logic?.handleDamage(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun handleBreak(event: PlayerItemBreakEvent) {
        val item = event.brokenItem
        item.logic?.handleBreak(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previous
        val equippedItem = event.now
        
        unequippedItem?.logic?.handleEquip(player, unequippedItem, false, event)
        equippedItem?.logic?.handleEquip(player, equippedItem, true, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun handleInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedItem = event.currentItem
        val cursorItem = event.cursor
        
        clickedItem?.logic?.handleInventoryClick(player, clickedItem, event)
        cursorItem?.logic?.handleInventoryClickOnCursor(player, cursorItem, event)
        
        if (event.click == ClickType.NUMBER_KEY) {
            val hotbarItem = player.inventory.getItem(event.hotbarButton)
            hotbarItem?.logic?.handleInventoryHotbarSwap(player, hotbarItem, event)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleBreakAction(event: BlockBreakActionEvent) {
        val player = event.player
        val item = event.player.inventory.itemInMainHand
        
        item.logic?.handleBlockBreakAction(player, item, event)
    }
    
    // This method stores the last used item for the RELEASE_USE_ITEM action below
    @PacketHandler(priority = EventPriority.HIGHEST, ignoreIfCancelled = true)
    private fun handleUseItem(event: ServerboundUseItemPacketEvent) {
        val player = event.player
        val item = player.inventory.getItem(event.hand.bukkitEquipmentSlot).takeUnlessEmpty()
        if (item != null)
            usedItems[player] = item
        else usedItems -= player
    }
    
    @PacketHandler(priority = EventPriority.HIGHEST, ignoreIfCancelled = true)
    private fun handleAction(event: ServerboundPlayerActionPacketEvent) {
        if (event.action == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            val player = event.player
            val item = usedItems[player]
            item?.logic?.handleRelease(player, item, event)
        }
    }
    
    private val ItemStack.logic: ItemLogic?
        get() = novaItem?.logic
    
}

