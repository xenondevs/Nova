package xyz.xenondevs.nova.item

import de.studiocode.invui.item.ItemBuilder
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.data.serialization.persistentdata.set
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.player.equipment.ArmorEquipEvent

/**
 * Handles actions performed on [ItemStack]s of a [NovaMaterial]
 */
abstract class NovaItem {
    
    open fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {}
    
    open fun handleBreak(player: Player, itemStack: ItemStack, event: PlayerItemBreakEvent) {}
    
    open fun handleEquip(player: Player, itemStack: ItemStack, equipped: Boolean, event: ArmorEquipEvent) {}
    
    open fun getDefaultItemBuilder(itemBuilder: ItemBuilder): ItemBuilder = itemBuilder
    
    inline fun <reified K> retrieveData(itemStack: ItemStack, key: NamespacedKey): K? {
        return itemStack.itemMeta?.persistentDataContainer?.get(key)
    }
    
    fun <T> retrieveData(itemStack: ItemStack, key: NamespacedKey, persistentDataType: PersistentDataType<*, T>): T? {
        return itemStack.itemMeta?.persistentDataContainer?.get(key, persistentDataType)
    }
    
    inline fun <reified T> storeData(itemStack: ItemStack, key: NamespacedKey, data: T?) {
        val itemMeta = itemStack.itemMeta
        val dataContainer = itemMeta?.persistentDataContainer
        if (dataContainer != null) {
            if (data != null) dataContainer.set(key, data)
            else dataContainer.remove(key)
            
            itemStack.itemMeta = itemMeta
        }
    }
    
    fun <T> storeData(itemStack: ItemStack, key: NamespacedKey, dataType: PersistentDataType<*, T>, data: T?) {
        val itemMeta = itemStack.itemMeta
        val dataContainer = itemMeta?.persistentDataContainer
        if (dataContainer != null) {
            if (data != null) dataContainer.set(key, dataType, data)
            else dataContainer.remove(key)
            
            itemStack.itemMeta = itemMeta
        }
    }
    
}