package xyz.xenondevs.nova.addon.registry

import org.bukkit.enchantments.Enchantment
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.world.item.enchantment.EnchantmentBuilder

@Deprecated(REGISTRIES_DEPRECATION)
interface EnchantmentRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun enchantment(name: String, enchantment: EnchantmentBuilder.() -> Unit): Provider<Enchantment> =
        addon.enchantment(name, enchantment)
    
}