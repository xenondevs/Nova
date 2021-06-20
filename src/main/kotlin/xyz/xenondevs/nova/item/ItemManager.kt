package xyz.xenondevs.nova.item

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Mob
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.equipment.ArmorEquipEvent
import xyz.xenondevs.nova.item.impl.BottledMobItem
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.addPrioritized
import xyz.xenondevs.nova.util.isCompletelyDenied
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.protection.ProtectionUtils

object ItemManager : Listener {
    
    fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun handleInteract(event: PlayerInteractEvent) {
        if (event.isCompletelyDenied()) return
        event.item?.novaMaterial?.novaItem?.handleInteract(event.player, event.item!!, event.action, event)
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun handleBreak(event: PlayerItemBreakEvent) {
        event.brokenItem.novaMaterial?.novaItem?.handleBreak(event.player, event.brokenItem, event)
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEquip(event: ArmorEquipEvent) {
        val player = event.player
        val unequippedItem = event.previousArmorItem
        val equippedItem = event.newArmorItem
        unequippedItem?.novaMaterial?.novaItem?.handleEquip(player, unequippedItem, false, event)
        equippedItem?.novaMaterial?.novaItem?.handleEquip(player, equippedItem, true, event)
    }
    
    // This should probably be somewhere else
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun handleEntityInteract(event: PlayerInteractAtEntityEvent) {
        val clicked = event.rightClicked
        if (clicked is Mob) {
            val player = event.player
            val item = player.inventory.getItem(event.hand)
            
            if (item.type == Material.GLASS_BOTTLE && ProtectionUtils.canUse(player, clicked.location)) {
                val fakeDamageEvent = EntityDamageByEntityEvent(player, clicked, DamageCause.ENTITY_ATTACK, Double.MAX_VALUE)
                Bukkit.getPluginManager().callEvent(fakeDamageEvent)
                
                if (!fakeDamageEvent.isCancelled && fakeDamageEvent.damage != 0.0) {
                    val itemStack = NovaMaterial.BOTTLED_MOB.createItemStack()
                    BottledMobItem.absorbEntity(itemStack, clicked)
                    
                    player.inventory.getItem(event.hand).amount -= 1
                    player.inventory.addPrioritized(event.hand, itemStack)
                    
                    if (event.hand == EquipmentSlot.HAND) player.swingMainHand() else player.swingOffHand()
                    
                    event.isCancelled = true
                }
            }
        }
    }
    
}