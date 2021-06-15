package xyz.xenondevs.nova.util

import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerPlayer
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
import xyz.xenondevs.nova.util.ReflectionUtils.nmsStack
import net.minecraft.world.entity.Entity as NMSEntity
import net.minecraft.world.entity.decoration.ArmorStand as NMSArmorStand
import net.minecraft.world.item.ItemStack as NMSItemStack

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
    
    @Suppress("UNCHECKED_CAST")
    fun spawnArmorStandSilently(
        location: Location,
        headStack: ItemStack,
        light: Boolean = true,
        modify: (ArmorStand.() -> Unit)? = null
    ): ArmorStand {
        val world = location.world!!
        
        // create EntityArmorStand
        val nmsArmorStand = ReflectionUtils.createNMSEntity(world, location, EntityType.ARMOR_STAND) as NMSArmorStand
        
        // set head item silently
        val armorItems = ReflectionRegistry.ARMOR_STAND_ARMOR_ITEMS_FIELD.get(nmsArmorStand) as NonNullList<NMSItemStack>
        armorItems[3] = headStack.nmsStack
        
        // get CraftArmorStand
        val armorStand = nmsArmorStand.bukkitEntity as ArmorStand
        
        // set other properties
        armorStand.isMarker = true
        armorStand.isVisible = false
        if (light) armorStand.fireTicks = Int.MAX_VALUE
        
        // set data
        if (modify != null) armorStand.modify()
        
        // add ArmorStand to world
        ReflectionUtils.addNMSEntityToWorld(world, nmsArmorStand)
        
        return armorStand
    }
    
}