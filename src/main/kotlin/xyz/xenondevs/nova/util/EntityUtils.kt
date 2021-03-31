package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

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
        
        // set data
        if (modify != null) armorStand.modify()
        
        // add ArmorStand to world
        ReflectionUtils.addNMSEntityToWorld(world, nmsArmorStand)
        
        return armorStand
    }
    
}