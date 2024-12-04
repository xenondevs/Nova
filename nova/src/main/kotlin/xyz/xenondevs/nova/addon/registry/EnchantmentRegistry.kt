package xyz.xenondevs.nova.addon.registry

import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.map
import xyz.xenondevs.nova.world.item.enchantment.EnchantmentBuilder

interface EnchantmentRegistry : AddonGetter {
    
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Provider<Enchantment> {
        val builder = EnchantmentBuilder(addon, name)
        builder.enchantment()
        return builder.register().map(CraftEnchantment::minecraftToBukkit)
    }
    
}