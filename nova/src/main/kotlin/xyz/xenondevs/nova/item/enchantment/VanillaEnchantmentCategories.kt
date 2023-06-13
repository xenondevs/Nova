package xyz.xenondevs.nova.item.enchantment

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.set
import net.minecraft.world.item.enchantment.EnchantmentCategory as MojangEnchantmentCategory

object VanillaEnchantmentCategories {
    
    val ARMOR = register(MojangEnchantmentCategory.ARMOR)
    val ARMOR_FEET = register(MojangEnchantmentCategory.ARMOR_FEET)
    val ARMOR_LEGS = register(MojangEnchantmentCategory.ARMOR_LEGS)
    val ARMOR_CHEST = register(MojangEnchantmentCategory.ARMOR_CHEST)
    val ARMOR_HEAD = register(MojangEnchantmentCategory.ARMOR_HEAD)
    val WEAPON = register(MojangEnchantmentCategory.WEAPON)
    val DIGGER = register(MojangEnchantmentCategory.DIGGER)
    val FISHING_ROD = register(MojangEnchantmentCategory.FISHING_ROD)
    val TRIDENT = register(MojangEnchantmentCategory.TRIDENT)
    val BREAKABLE = register(MojangEnchantmentCategory.BREAKABLE)
    val BOW = register(MojangEnchantmentCategory.BOW)
    val WEARABLE = register(MojangEnchantmentCategory.WEARABLE)
    val CROSSBOW = register(MojangEnchantmentCategory.CROSSBOW)
    val VANISHABLE = register(MojangEnchantmentCategory.VANISHABLE)
    
    private fun register(vanillaCategory: MojangEnchantmentCategory): EnchantmentCategory {
        val id = ResourceLocation("minecraft", vanillaCategory.name.lowercase())
        val category = VanillaEnchantmentCategory(id, vanillaCategory)
        NovaRegistries.ENCHANTMENT_CATEGORY[id] = category
        return category
    }
    
}