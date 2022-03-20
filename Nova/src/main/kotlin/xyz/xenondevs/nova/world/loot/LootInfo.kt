package xyz.xenondevs.nova.world.loot

import org.bukkit.NamespacedKey
import xyz.xenondevs.nova.material.ItemNovaMaterial

class LootInfo(
    val frequency: Double,
    val min: Int,
    val max: Int,
    val item: ItemNovaMaterial,
    val whitelisted: List<NamespacedKey>,
    val blacklisted: List<NamespacedKey>
) {
    
    fun isWhitelisted(key: NamespacedKey): Boolean {
        if (blacklisted.isNotEmpty())
            return key !in blacklisted
        return whitelisted.isEmpty() || key in whitelisted
    }
    
}