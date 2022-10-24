package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.player.equipment.ArmorType

@HardcodedMaterialOptions
fun WearableOptions(
    armorType: ArmorType,
    armor: Double = 0.0,
    armorToughness: Double = 0.0,
    knockbackResistance: Double = 0.0
): WearableOptions = HardcodedWearableOptions(armorType, armor, armorToughness, knockbackResistance)

sealed interface WearableOptions {
    
    val armorType: ArmorType
    val armor: Double
    val armorToughness: Double
    val knockbackResistance: Double
    
    companion object : MaterialOptionsType<WearableOptions> {
        
        override fun configurable(material: ItemNovaMaterial): WearableOptions =
             ConfigurableWearableOptions(material)
        
        override fun configurable(path: String): WearableOptions =
            ConfigurableWearableOptions(path)
        
        fun semiConfigurable(armorType: ArmorType, material: ItemNovaMaterial): WearableOptions =
            SemiConfigurableWearableOptions(armorType, material)
        
        fun semiConfigurable(armorType: ArmorType, path: String): WearableOptions =
            SemiConfigurableWearableOptions(armorType, path)
        
    }
    
}

private class HardcodedWearableOptions(
    override val armorType: ArmorType,
    override val armor: Double,
    override val armorToughness: Double,
    override val knockbackResistance: Double
) : WearableOptions

private class ConfigurableWearableOptions : ConfigAccess, WearableOptions {
    
    override val armorType by getEntry<String>("armor_type").map { ArmorType.valueOf(it.uppercase()) }
    override val armor by getOptionalEntry<Double>("armor").orElse(0.0)
    override val armorToughness by getOptionalEntry<Double>("armor_toughness").orElse(0.0)
    override val knockbackResistance by getOptionalEntry<Double>("knockback_resistance").orElse(0.0)
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}

private class SemiConfigurableWearableOptions : ConfigAccess, WearableOptions {
    
    override val armorType: ArmorType
    override val armor by getOptionalEntry<Double>("armor").orElse(0.0)
    override val armorToughness by getOptionalEntry<Double>("armor_toughness").orElse(0.0)
    override val knockbackResistance by getOptionalEntry<Double>("knockback_resistance").orElse(0.0)
    
    constructor(armorType: ArmorType, path: String) : super(path) {
        this.armorType = armorType
    }
    
    constructor(armorType: ArmorType, material: ItemNovaMaterial) : super(material) {
        this.armorType = armorType
    }
    
}