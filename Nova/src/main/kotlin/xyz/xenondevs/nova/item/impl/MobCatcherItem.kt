package xyz.xenondevs.nova.item.impl

import de.studiocode.invui.item.builder.ItemBuilder
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getTargetLocation

private val DATA_KEY = NamespacedKey(NOVA, "entityData")
private val TYPE_KEY = NamespacedKey(NOVA, "entityType")
private val TIME_KEY = NamespacedKey(NOVA, "fillTime")

private val BLACKLISTED_ENTITY_TYPES = DEFAULT_CONFIG
    .getArray("bottled_mob_blacklist")!!
    .getAllStrings()
    .mapTo(HashSet(), EntityType::valueOf)

object MobCatcherItem : NovaItem() {
    
    override fun handleEntityInteract(player: Player, itemStack: ItemStack, clicked: Entity, event: PlayerInteractAtEntityEvent) {
        if (clicked is Mob
            && clicked.type !in BLACKLISTED_ENTITY_TYPES
            && ProtectionManager.canInteractWithEntity(player, clicked, itemStack)
            && getEntityData(itemStack) == null
        ) {
            
            val fakeDamageEvent = EntityDamageByEntityEvent(player, clicked, EntityDamageEvent.DamageCause.ENTITY_ATTACK, Double.MAX_VALUE)
            Bukkit.getPluginManager().callEvent(fakeDamageEvent)
            
            if (!fakeDamageEvent.isCancelled && fakeDamageEvent.damage != 0.0) {
                val newCatcher = NovaMaterialRegistry.MOB_CATCHER.createItemStack()
                absorbEntity(newCatcher, clicked)
                
                player.inventory.getItem(event.hand)!!.amount -= 1
                player.inventory.addPrioritized(event.hand, newCatcher)
                
                if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                
                event.isCancelled = true
            }
            
        }
    }
    
    override fun handleInteract(player: Player, itemStack: ItemStack, action: Action, event: PlayerInteractEvent) {
        if (action == Action.RIGHT_CLICK_BLOCK) {
            // Adds a small delay to prevent players from spamming the item
            if (System.currentTimeMillis() - (retrieveData<Long>(itemStack, TIME_KEY) ?: -1) < 50) return
            
            val data = getEntityData(itemStack)
            if (data != null) {
                val location = player.eyeLocation.getTargetLocation(0.25, 8.0)
                
                if (ProtectionManager.canUseItem(player, itemStack, location)) {
                    player.inventory.getItem(event.hand!!)!!.amount -= 1
                    player.inventory.addPrioritized(event.hand!!, NovaMaterialRegistry.MOB_CATCHER.createItemStack())
                    
                    
                    EntityUtils.deserializeAndSpawn(data, location)
                    if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                    
                    event.isCancelled = true
                }
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
        
        itemStack.itemMeta = ItemBuilder(itemStack).addLoreLines(localized(
            ChatColor.DARK_GRAY,
            "item.nova.mob_catcher.type",
            localized(ChatColor.YELLOW, entity)
        )).get().itemMeta
    }
    
}