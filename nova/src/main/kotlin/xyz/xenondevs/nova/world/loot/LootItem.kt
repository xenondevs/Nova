package xyz.xenondevs.nova.world.loot

import de.studiocode.invui.item.builder.ItemBuilder

data class LootItem(
    val item: ItemBuilder,
    val chance: Double,
    val amount: IntRange
)