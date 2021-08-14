package xyz.xenondevs.nova.item.impl

import de.studiocode.invui.item.ItemBuilder
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.capitalizeAll
import xyz.xenondevs.nova.util.data.addLoreLines
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getTargetLocation
import xyz.xenondevs.nova.integration.protection.ProtectionManager

private val DATA_KEY = NamespacedKey(NOVA, "entityData")
private val TYPE_KEY = NamespacedKey(NOVA, "entityType")
private val TIME_KEY = NamespacedKey(NOVA, "fillTime")

private val BLACKLISTED_ENTITY_TYPES = NovaConfig
    .getArray("bottled_mob_blacklist")!!
    .getAllStrings()
    .mapTo(HashSet(), EntityType::valueOf)

object BottledMobItem : NovaItem(), Listener {
    
    init {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val clicked = event.rightClicked
        if (clicked is Mob) {
            val player = event.player
            val item = player.inventory.getItem(event.hand)
            
            if (item.type == Material.GLASS_BOTTLE
                && !BLACKLISTED_ENTITY_TYPES.contains(clicked.type)
                && ProtectionManager.canUse(player, clicked.location)) {
                
                val fakeDamageEvent = EntityDamageByEntityEvent(player, clicked, DamageCause.ENTITY_ATTACK, Double.MAX_VALUE)
                Bukkit.getPluginManager().callEvent(fakeDamageEvent)
                
                if (!fakeDamageEvent.isCancelled && fakeDamageEvent.damage != 0.0) {
                    val itemStack = NovaMaterial.BOTTLED_MOB.createItemStack()
                    absorbEntity(itemStack, clicked)
                    
                    player.inventory.getItem(event.hand).amount -= 1
                    player.inventory.addPrioritized(event.hand, itemStack)
                    
                    if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                    
                    event.isCancelled = true
                }
            }
        }
    }
    
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
        
        itemStack.itemMeta = ItemBuilder(itemStack).addLoreLines(localized(
            ChatColor.DARK_GRAY,
            "item.nova.bottled_mob.type",
            coloredText(ChatColor.YELLOW, entity.type.name.lowercase().replace('_', ' ').capitalizeAll())
        )).get().itemMeta
    }
    
}