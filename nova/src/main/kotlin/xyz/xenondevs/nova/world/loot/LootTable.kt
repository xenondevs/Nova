package xyz.xenondevs.nova.world.loot

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

class LootTable(
    private val items: List<LootItem>,
    private val whitelist: List<NamespacedKey>,
    private val blacklist: List<NamespacedKey>
) {
    
    fun isAllowed(key: NamespacedKey): Boolean {
        if (blacklist.isNotEmpty())
            return key !in blacklist
        return whitelist.isEmpty() || key in whitelist
    }
    
    fun getRandomItems(): List<ItemStack> {
        return items
            .asSequence()
            .filter { Math.random() * 100 <= it.chance }
            .map { it.item.setAmount(it.amount.random()).get() }
            .toList()
    }
}