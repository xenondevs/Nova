package xyz.xenondevs.nova.world.item.logic

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import org.bukkit.Bukkit
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
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundPlayerActionPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundUseItemPacketEvent
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.block.event.BlockBreakActionEvent
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.player.equipment.ArmorEquipEvent
import java.util.*

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.SYNC
)
internal object ItemListener : Listener, PacketListener {
    
    private val usedItems = WeakHashMap<Player, ItemStack>()
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
        runTaskTimer(0, 1, ::handleTick)
    }
    
    private fun handleTick() {
        for (player in Bukkit.getOnlinePlayers()) {
            for ((slot, itemStack) in player.inventory.contents.withIndex()) {
                val novaItem = itemStack?.novaItem
                    ?: continue
                novaItem.handleInventoryTick(player, itemStack, slot)
            }
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    private fun handleInteract(wrappedEvent: WrappedPlayerInteractEvent) {
        val event = wrappedEvent.event
        val player = event.player
        val item = event.item
        
        val location = event.clickedBlock?.location ?: player.location
        if (item == null || !ProtectionManager.canUseItem(player, item, location))
            return
        
        item.novaItem?.handleInteract(event.player, item, event.action, wrappedEvent)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val item = event.player.inventory.getItem(event.hand).takeUnlessEmpty()
        item?.novaItem?.handleEntityInteract(event.player, item, event.rightClicked, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEntityAttack(event: EntityDamageByEntityEvent) {
        val player = event.damager as? Player ?: return
        val item = player.inventory.getItem(EquipmentSlot.HAND).takeUnlessEmpty()
        item?.novaItem?.handleAttackEntity(player, item, event.entity, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleBlockBreak(event: BlockBreakEvent) {
        val item = event.player.inventory.getItem(EquipmentSlot.HAND).takeUnlessEmpty()
        item?.novaItem?.handleBreakBlock(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleDamage(event: PlayerItemDamageEvent) {
        val item = event.item
        item.novaItem?.handleDamage(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun handleBreak(event: PlayerItemBreakEvent) {
        val item = event.brokenItem
        item.novaItem?.handleBreak(event.player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previous
        val equippedItem = event.now
        
        unequippedItem?.novaItem?.handleEquip(player, unequippedItem, false, event)
        equippedItem?.novaItem?.handleEquip(player, equippedItem, true, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private fun handleInteract(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val clickedItem = event.currentItem
        val cursorItem = event.cursor.takeUnlessEmpty()
        
        clickedItem?.novaItem?.handleInventoryClick(player, clickedItem, event)
        cursorItem?.novaItem?.handleInventoryClickOnCursor(player, cursorItem, event)
        
        if (event.click == ClickType.NUMBER_KEY) {
            val hotbarItem = player.inventory.getItem(event.hotbarButton)
            hotbarItem?.novaItem?.handleInventoryHotbarSwap(player, hotbarItem, event)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleBreakAction(event: BlockBreakActionEvent) {
        val player = event.player
        val item = event.player.inventory.itemInMainHand
        
        item.novaItem?.handleBlockBreakAction(player, item, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleConsume(event: PlayerItemConsumeEvent) {
        val player = event.player
        val item = event.item
        
        item.novaItem?.handleConsume(player, item, event)
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
            item?.novaItem?.handleRelease(player, item, event)
        }
    }
    
}

