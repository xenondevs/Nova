package xyz.xenondevs.nova.world.loot

import xyz.xenondevs.invui.item.builder.ItemBuilder

data class LootItem(
    val item: ItemBuilder,
    val chance: Double,
    val amount: IntRange
)