package xyz.xenondevs.nova.item.impl

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.*

private val DATA_KEY = NamespacedKey(NOVA, "entityData")
private val TYPE_KEY = NamespacedKey(NOVA, "entityType")
private val TIME_KEY = NamespacedKey(NOVA, "fillTime")

object BottledMobItem : NovaItem() {
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            // prevents picking up and instantly pacing entity again
            if (System.currentTimeMillis() - (retrieveData<Long>(itemStack, TIME_KEY) ?: -1) < 50) return
            
            val data = getEntityData(itemStack)
            if (data != null) {
                player.inventory.getItem(event.hand!!).amount -= 1
                player.inventory.addPrioritized(event.hand!!, ItemStack(Material.GLASS_BOTTLE))
                
                val location = player.eyeLocation.getTargetLocation(0.25, 8.0)
                
                EntityUtils.deserializeAndSpawn(data, location)
                if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                
                event.isCancelled = true
            }
        }
    }
    
    fun getEntityData(itemStack: ItemStack): ByteArray? = retrieveData(itemStack, DATA_KEY)
    
    fun getEntityType(itemStack: ItemStack): EntityType? = retrieveData<String>(itemStack, TYPE_KEY)?.let { EntityType.valueOf(it) }
    
    fun setEntityData(itemStack: ItemStack, type: EntityType, data: ByteArray) {
        storeData(itemStack, DATA_KEY, data)
        storeData(itemStack, TYPE_KEY, type.name)
        storeData(itemStack, TIME_KEY, System.currentTimeMillis())
    }
    
    fun absorbEntity(itemStack: ItemStack, entity: Entity) {
        val data = EntityUtils.serialize(entity, true)
        setEntityData(itemStack, entity.type, data)
        
        val itemMeta = itemStack.itemMeta!!
        itemMeta.lore = listOf("§8Type: §e${entity.type.name.lowercase().replace('_', ' ').capitalizeAll()}")
        
        itemStack.itemMeta = itemMeta
    }
    
}