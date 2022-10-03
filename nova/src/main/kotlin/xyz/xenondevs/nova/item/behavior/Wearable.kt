package xyz.xenondevs.nova.item.behavior

import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.player.equipment.ArmorType
import xyz.xenondevs.nova.player.equipment.EquipMethod
import xyz.xenondevs.nova.util.isPlayerView
import xyz.xenondevs.nova.util.item.takeUnlessAir

class Wearable(private val type: ArmorType, private val armor: Double = 0.0, private val armorToughness: Double = 0.0) : ItemBehavior() {
    
    override fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {
        if (equipped) {
            player.getAttribute(Attribute.GENERIC_ARMOR)!!.baseValue += armor
            player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)!!.baseValue += armorToughness
        } else {
            player.getAttribute(Attribute.GENERIC_ARMOR)!!.baseValue -= armor
            player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS)!!.baseValue -= armorToughness
        }
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_AIR
            && player.inventory.getItem(type.equipmentSlot)?.takeUnlessAir() == null
            && !callArmorEquipEvent(player, EquipMethod.RIGHT_CLICK_EQUIP, null, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(type.equipmentSlot, itemStack)
            player.inventory.setItem(event.hand!!, null)
        }
    }
    
    @Suppress("DEPRECATION")
    override fun handleInventoryClickOnCursor(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        val slotType = event.slotType
        val currentItem = event.currentItem
        if (slotType == InventoryType.SlotType.ARMOR
            && event.rawSlot == type.rawSlot
            && (event.click == ClickType.LEFT || event.click == ClickType.RIGHT)
            && !callArmorEquipEvent(player, EquipMethod.SWAP, currentItem, itemStack)
        ) {
            event.isCancelled = true
            
            player.inventory.setItem(type.equipmentSlot, itemStack)
            event.cursor = currentItem
        }
    }
    
    override fun handleInventoryClick(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        if ((event.click == ClickType.SHIFT_LEFT || event.click == ClickType.SHIFT_RIGHT)
            && event.view.isPlayerView()
            && player.inventory.getItem(type.equipmentSlot)?.takeUnlessAir() == null
            && !callArmorEquipEvent(player, EquipMethod.SHIFT_CLICK, null, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(type.equipmentSlot, itemStack)
            event.view.setItem(event.rawSlot, null)
        }
    }
    
    override fun handleInventoryHotbarSwap(player: Player, itemStack: ItemStack, event: InventoryClickEvent) {
        val currentItem = event.currentItem
        if (event.slotType == InventoryType.SlotType.ARMOR
            && event.rawSlot == type.rawSlot
            && !callArmorEquipEvent(player, EquipMethod.HOTBAR_SWAP, currentItem, itemStack)
        ) {
            event.isCancelled = true
            player.inventory.setItem(type.equipmentSlot, itemStack)
            player.inventory.setItem(event.hotbarButton, currentItem)
        }
    }
    
    private fun callArmorEquipEvent(player: Player, method: EquipMethod, previous: ItemStack?, now: ItemStack?): Boolean {
        val event = ArmorEquipEvent(player, method, previous, now)
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }
    
}