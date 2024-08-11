package xyz.xenondevs.nova.world.loot

import org.bukkit.inventory.ItemStack

data class LootItem(
    val item: ItemStack,
    val chance: Double,
    val amount: IntRange
)