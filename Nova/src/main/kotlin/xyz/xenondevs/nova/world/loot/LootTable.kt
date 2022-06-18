package xyz.xenondevs.nova.world.loot

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

class LootTable(
    val items: List<LootItem>,
    val whitelisted: List<NamespacedKey>,
    val blacklisted: List<NamespacedKey>
) {
    
    fun isWhitelisted(key: NamespacedKey): Boolean {
        if (blacklisted.isNotEmpty())
            return key !in blacklisted
        return whitelisted.isEmpty() || key in whitelisted
    }
    
    fun getRandomItems(): List<ItemStack> {
        return items
            .asSequence()
            .filter { Math.random() * 100 <= it.chance }
            .map { it.item.setAmount(it.amount.random()).get() }
            .toList()
    }
}