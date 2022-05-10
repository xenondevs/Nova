package xyz.xenondevs.nova.world.loot

import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.loot.LootTables
import xyz.xenondevs.nova.material.ItemNovaMaterial

@DslMarker
annotation class LootDsl

class LootInfo(
    val frequency: Double,
    val amount: IntRange,
    val item: ItemBuilder,
    val whitelisted: List<NamespacedKey>,
    val blacklisted: List<NamespacedKey>
) {
    
    fun isWhitelisted(key: NamespacedKey): Boolean {
        if (blacklisted.isNotEmpty())
            return key !in blacklisted
        return whitelisted.isEmpty() || key in whitelisted
    }
    
    fun getRandomAmount(): Int {
        return amount.random()
    }
    
    @LootDsl
    class Builder {
        
        private var frequency: Double? = null
        private var amount = 1..1
        private var item: ItemBuilder? = null
        private var whitelisted: MutableList<NamespacedKey> = mutableListOf()
        private var blacklisted: MutableList<NamespacedKey> = mutableListOf()
        
        /**
         * Sets the frequency of the loot (1.0 = 100.0% chance)
         */
        fun frequency(frequency: Double) {
            this.frequency = frequency
        }
    
        /**
         * Sets the frequency of the loot (1 = 100% chance)
         */
        fun frequency(frequency: Int) {
            this.frequency = frequency.toDouble()
        }
        
        /**
         * Sets the minimum and maximum amount of loot
         */
        fun amount(amount: IntRange) {
            this.amount = amount
        }
        
        /**
         * Sets the minimum and maximum amount of loot
         */
        fun amount(min: Int, max: Int) {
            this.amount = min..max
        }
        
        /**
         * Sets the exact amount of loot
         */
        fun amount(amount: Int) {
            this.amount = amount..amount
        }
        
        /**
         * Sets the item to be looted
         */
        fun item(item: ItemStack) {
            this.item = ItemBuilder(item)
        }
        
        /**
         * Sets the item to be looted
         */
        fun item(material: ItemNovaMaterial) {
            this.item = material.createItemBuilder()
        }
        
        /**
         * Adds a whitelisted loot table
         */
        fun whitelist(namespacedKey: NamespacedKey) {
            whitelisted += namespacedKey
        }
        
        /**
         * Adds a whitelisted loot table
         */
        fun whitelist(vararg namespacedKeys: String) {
            namespacedKeys.forEach { keyStr ->
                val key =
                    if (keyStr.contains(':')) NamespacedKey.fromString(keyStr)
                    else NamespacedKey.minecraft(keyStr)
    
                whitelisted += key
                    ?: throw IllegalArgumentException("Invalid namespaced key: $keyStr")
            }
        }
    
        /**
         * Adds a whitelisted loot table
         */
        fun whitelist(vararg namespacedKeys: NamespacedKey) {
            whitelisted += namespacedKeys.toList()
        }
        
        fun whitelist(vararg lootTables: LootTables) {
            whitelisted += lootTables.map { it.key }
        }
        
        /**
         * Adds a blacklisted loot table
         */
        fun blacklist(namespacedKey: NamespacedKey) {
            blacklisted += namespacedKey
        }
        
        /**
         * Adds a blacklisted loot table
         */
        fun blacklist(vararg namespacedKeys: String) {
            namespacedKeys.forEach { keyStr ->
                val key =
                    if (keyStr.contains(':')) NamespacedKey.fromString(keyStr)
                    else NamespacedKey.minecraft(keyStr)
    
                blacklisted += key
                    ?: throw IllegalArgumentException("Invalid namespaced key: $keyStr")
            }
        }
    
        /**
         * Adds a blacklisted loot table
         */
        fun blacklist(vararg namespacedKeys: NamespacedKey) {
            blacklisted += namespacedKeys.toList()
        }
        
        fun blacklist(vararg lootTables: LootTables) {
            blacklisted += lootTables.map { it.key }
        }
        
        fun build(): LootInfo {
            return LootInfo(frequency!!, amount, item!!, whitelisted, blacklisted)
        }
        
    }
}