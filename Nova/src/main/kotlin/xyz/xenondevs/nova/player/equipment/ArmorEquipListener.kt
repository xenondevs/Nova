package xyz.xenondevs.nova.player.equipment

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseArmorEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType.SlotType
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.isPlayerView
import xyz.xenondevs.nova.util.isRightClick
import xyz.xenondevs.nova.util.runTask

private fun ItemStack?.getNullIfAir(): ItemStack? {
    return if (this?.type != Material.AIR) this else null
}

object ArmorEquipListener : Initializable(), Listener {
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    override fun init() {
        LOGGER.info("Initializing ArmorEquipListener")
        Bukkit.getPluginManager().registerEvents(this, NOVA)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInventoryClick(event: InventoryClickEvent) {
        val view = event.view
        val player = event.whoClicked as Player
        val currentItem = event.currentItem.getNullIfAir()
        val cursorItem = event.cursor.getNullIfAir()
        val slotType = event.slotType
        
        var equipEvent: ArmorEquipEvent? = null
        
        when (event.action) {
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE -> {
                if (slotType == SlotType.ARMOR) {
                    equipEvent = ArmorEquipEvent(player, EquipMethod.NORMAL_CLICK, currentItem, cursorItem)
                }
            }
            
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE -> {
                if (slotType == SlotType.ARMOR && ArmorType.fitsOnSlot(cursorItem, event.rawSlot)) {
                    equipEvent = ArmorEquipEvent(player, EquipMethod.NORMAL_CLICK, currentItem, cursorItem)
                }
            }
            
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT -> {
                if (slotType == SlotType.ARMOR) {
                    equipEvent = ArmorEquipEvent(player, EquipMethod.DROP, currentItem, null)
                }
            }
            
            InventoryAction.SWAP_WITH_CURSOR -> {
                if (slotType == SlotType.ARMOR && ArmorType.fitsOnSlot(cursorItem, event.rawSlot)) {
                    equipEvent = ArmorEquipEvent(player, EquipMethod.SWAP, currentItem, cursorItem)
                }
            }
            
            InventoryAction.HOTBAR_SWAP -> {
                if (slotType == SlotType.ARMOR) {
                    val hotbarItem = player.inventory.getItem(event.hotbarButton)
                    if (ArmorType.fitsOnSlot(hotbarItem, event.rawSlot)) {
                        equipEvent = ArmorEquipEvent(player, EquipMethod.HOTBAR_SWAP, currentItem, hotbarItem)
                    }
                }
            }
            
            InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                if (slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR) {
                    
                    // check that it's actually the players own inventory that's open here
                    if (view.isPlayerView()) {
                        
                        // check that the clicked item is armor
                        val armorType = ArmorType.of(currentItem)
                        if (armorType != null) {
                            
                            // check that the armor slot for that armor type is empty
                            val currentArmorPiece = view.getItem(armorType.rawSlot).getNullIfAir()
                            if (currentArmorPiece == null) {
                                equipEvent = ArmorEquipEvent(player, EquipMethod.SHIFT_CLICK, null, currentItem)
                            }
                        }
                    }
                } else if (slotType == SlotType.ARMOR) {
                    equipEvent = ArmorEquipEvent(player, EquipMethod.SHIFT_CLICK, currentItem, null)
                }
            }
            
            else -> Unit
        }
        
        if (equipEvent != null) {
            Bukkit.getPluginManager().callEvent(equipEvent)
            event.isCancelled = equipEvent.isCancelled
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleInteract(e: WrappedPlayerInteractEvent) {
        val event = e.event
        if (event.isCompletelyDenied()) return
        
        val item = event.item
        if (event.action.isRightClick() && item != null) {
            val armorType = ArmorType.of(item)
            if (armorType != null) {
                val currentArmorPiece = event.player.equipment?.getItem(armorType.equipmentSlot).getNullIfAir()
                if (currentArmorPiece == null) {
                    val equipEvent = ArmorEquipEvent(event.player, EquipMethod.RIGHT_CLICK_EQUIP, null, item)
                    Bukkit.getPluginManager().callEvent(equipEvent)
                    event.isCancelled = equipEvent.isCancelled
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleInventoryDrag(event: InventoryDragEvent) {
        if (event.view.isPlayerView()) {
            var equipEvent: ArmorEquipEvent? = null
            
            for (slot in event.rawSlots) {
                val armorType = ArmorType.of(slot)
                if (armorType != null) {
                    val player = event.whoClicked as Player
                    val currentItem = player.equipment?.getItem(armorType.equipmentSlot).getNullIfAir()
                    if (currentItem == null) {
                        equipEvent = ArmorEquipEvent(event.whoClicked as Player, EquipMethod.DRAG, null, event.newItems[slot])
                        break
                    }
                }
            }
            
            if (equipEvent != null) {
                Bukkit.getPluginManager().callEvent(equipEvent)
                event.isCancelled = equipEvent.isCancelled
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handlePlayerDeath(event: PlayerDeathEvent) {
        if (!event.keepInventory) {
            val player = event.entity
            val equipment = player.equipment
            ArmorType.ARMOR_EQUIPMENT_SLOTS.forEach {
                val armorItem = equipment?.getItem(it).getNullIfAir()
                if (armorItem != null) {
                    val equipEvent = ArmorEquipEvent(player, EquipMethod.DEATH, armorItem, null, false)
                    Bukkit.getPluginManager().callEvent(equipEvent)
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleItemBreak(event: PlayerItemBreakEvent) {
        val armorItem = event.brokenItem
        val armorType = ArmorType.of(armorItem)
        if (armorType != null) {
            val equipEvent = ArmorEquipEvent(event.player, EquipMethod.BREAK, armorItem, null)
            Bukkit.getPluginManager().callEvent(equipEvent)
            if (equipEvent.isCancelled) {
                val itemMeta = armorItem.itemMeta
                if (itemMeta is Damageable) {
                    runTask {
                        armorItem.amount = 1
                        itemMeta.damage -= 1
                        armorItem.itemMeta = itemMeta
                    }
                }
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleArmorDispense(event: BlockDispenseArmorEvent) {
        val entity = event.targetEntity
        if (entity is Player) {
            val equipEvent = ArmorEquipEvent(entity, EquipMethod.DISPENSER, null, event.item)
            Bukkit.getPluginManager().callEvent(equipEvent)
            event.isCancelled = equipEvent.isCancelled
        }
    }
    
}