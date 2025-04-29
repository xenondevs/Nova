package xyz.xenondevs.nova.addon.registry

import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.util.Key
import xyz.xenondevs.nova.world.item.enchantment.EnchantmentBuilder

interface EnchantmentRegistry : AddonGetter {
    
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Provider<Enchantment> =
        EnchantmentBuilder(Key(addon, name)).apply(enchantment).register()
    
}