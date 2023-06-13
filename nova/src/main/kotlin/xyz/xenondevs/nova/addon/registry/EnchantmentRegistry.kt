package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.enchantment.EnchantmentBuilder

interface EnchantmentRegistry : AddonGetter {
    
    fun enchantment(name: String) =
        EnchantmentBuilder(addon, name)
    
}