package xyz.xenondevs.nova.material.options

import net.minecraft.world.item.enchantment.EnchantmentCategory

data class EnchantOptions(
    val enchantmentValue: Int,
    val enchantmentCategories: List<EnchantmentCategory>
)