package xyz.xenondevs.nova.addon.registry

import org.bukkit.craftbukkit.enchantments.CraftEnchantment
import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.nova.item.enchantment.EnchantmentBuilder

interface EnchantmentRegistry : AddonGetter {
    
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Enchantment {
        val builder = EnchantmentBuilder(addon, name)
        builder.enchantment()
        return CraftEnchantment.minecraftToBukkit(builder.register())
    }
    
}