package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.item.enchantment.EnchantmentCategoryBuilder

interface EnchantmentCategoryRegistry : AddonGetter {
    
    fun registerEnchantmentCategory(name: String): EnchantmentCategory =
        enchantmentCategory(name).register()
    
    fun enchantmentCategory(name: String): EnchantmentCategoryBuilder =
        EnchantmentCategoryBuilder(addon, name)
    
}