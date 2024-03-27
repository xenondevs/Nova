package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.item.enchantment.EnchantmentCategoryBuilder

interface EnchantmentCategoryRegistry : AddonGetter {
    
    fun enchantmentCategory(name: String, enchantmentCategory: EnchantmentCategoryBuilder.() -> Unit = {}): EnchantmentCategory =
        EnchantmentCategoryBuilder(addon, name).apply(enchantmentCategory).register()
    
}