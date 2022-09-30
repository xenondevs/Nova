package xyz.xenondevs.nova.material.options

import net.minecraft.world.item.enchantment.EnchantmentCategory
import xyz.xenondevs.nova.data.config.provider.ConfigAccess
import xyz.xenondevs.nova.data.config.provider.map
import xyz.xenondevs.nova.material.ItemNovaMaterial

@HardcodedMaterialOptions
fun EnchantableOptions(
    enchantmentValue: Int,
    enchantmentCategories: List<EnchantmentCategory>
): EnchantableOptions = HardcodedEnchantableOptions(enchantmentValue, enchantmentCategories)

sealed interface EnchantableOptions {
    
    val enchantmentValue: Int
    val enchantmentCategories: List<EnchantmentCategory>
    
    companion object : MaterialOptionsType<EnchantableOptions> {
        
        override fun configurable(material: ItemNovaMaterial): EnchantableOptions =
            ConfigurableEnchantableOptions(material)
        
        override fun configurable(path: String): EnchantableOptions =
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
    constructor(material: ItemNovaMaterial) : super(material)
    
}