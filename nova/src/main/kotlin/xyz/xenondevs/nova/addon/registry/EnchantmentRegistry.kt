package xyz.xenondevs.nova.addon.registry

import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.EnchantmentBuilder

interface EnchantmentRegistry : AddonGetter {
    
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Enchantment =
        EnchantmentBuilder(addon, name).apply(enchantment).register()
    
}