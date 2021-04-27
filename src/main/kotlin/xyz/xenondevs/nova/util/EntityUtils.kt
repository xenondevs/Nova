package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import xyz.xenondevs.nova.NOVA
import java.util.*

fun Player.awardAdvancement(key: NamespacedKey) {
    val advancement = Bukkit.getAdvancement(key)!!
    val progress = getAdvancementProgress(advancement)
    advancement.criteria.forEach { progress.awardCriteria(it) }
}

fun Entity.teleport(modifyLocation: Location.() -> Unit) {
    val location = location
    location.modifyLocation()
    teleport(location)
}

fun PersistentDataContainer.hasNovaData(): Boolean {
    val novaNameSpace = NOVA.name.lowercase()
    return keys.any { it.namespace == novaNameSpace }
}

object EntityUtils {
    
    fun spawnArmorStandSilently(
        location: Location,
        headStack: ItemStack,
        modify: (ArmorStand.() -> Unit)? = null
    ): ArmorStand {
        val world = location.world!!
        
        // create EntityArmorStand
        val nmsArmorStand = ReflectionUtils.createNMSEntity(world, location, EntityType.ARMOR_STAND)
        
        // set head item silently
        ReflectionUtils.setArmorStandArmorItems(nmsArmorStand, 3, ReflectionUtils.createNMSItemStackCopy(headStack))
        
        // get CraftArmorStand
        val armorStand = ReflectionUtils.createBukkitEntityFromNMSEntity(nmsArmorStand) as ArmorStand
        
        // set other properties
        armorStand.isMarker = true
        armorStand.isVisible = false
        armorStand.fireTicks = Int.MAX_VALUE
        
        // set data
        if (modify != null) armorStand.modify()
        
        // add ArmorStand to world
        ReflectionUtils.addNMSEntityToWorld(world, nmsArmorStand)
        
        return armorStand
    }
    
}