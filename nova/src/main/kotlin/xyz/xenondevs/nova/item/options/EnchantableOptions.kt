package xyz.xenondevs.nova.item.options

import net.minecraft.world.item.enchantment.EnchantmentCategory
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem

@HardcodedMaterialOptions
fun EnchantableOptions(
    enchantmentValue: Int,
    enchantmentCategories: List<EnchantmentCategory>
): EnchantableOptions = HardcodedEnchantableOptions(enchantmentValue, enchantmentCategories)

sealed interface EnchantableOptions {
    
    val enchantmentValue: Int
    val enchantmentCategories: List<EnchantmentCategory>
    
    companion object {
        
        fun configurable(item: NovaItem): EnchantableOptions =
            ConfigurableEnchantableOptions(item)
        
        fun configurable(path: String): EnchantableOptions =
            ConfigurableEnchantableOptions(path)
        
    }
    
}

private class HardcodedEnchantableOptions(
    override val enchantmentValue: Int,
    override val enchantmentCategories: List<EnchantmentCategory>
) : EnchantableOptions

private class ConfigurableEnchantableOptions : ConfigAccess, EnchantableOptions {
    
    override val enchantmentValue by getEntry<Int>("enchantment_value")
    override val enchantmentCategories by getEntry<List<String>>("enchantment_categories")
        .map { list -> list.map { EnchantmentCategory.valueOf(it.uppercase()) } }
    
    constructor(path: String) : super(path)
    constructor(item: NovaItem) : super(item)
    
}