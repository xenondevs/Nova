package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.map
import xyz.xenondevs.nova.data.provider.orElse
import xyz.xenondevs.nova.data.provider.provider
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
    
    val armorTypeProvider: Provider<ArmorType>
    val armorProvider: Provider<Double>
    val armorToughnessProvider: Provider<Double>
    val knockbackResistanceProvider: Provider<Double>
    
    val armorType: ArmorType
        get() = armorTypeProvider.value
    val armor: Double
        get() = armorProvider.value
    val armorToughness: Double
        get() = armorToughnessProvider.value
    val knockbackResistance: Double
        get() = knockbackResistanceProvider.value
    
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
     armorType: ArmorType,
     armor: Double,
     armorToughness: Double,
     knockbackResistance: Double
) : WearableOptions {
    override val armorTypeProvider = provider(armorType)
    override val armorProvider = provider(armor)
    override val armorToughnessProvider = provider(armorToughness)
    override val knockbackResistanceProvider = provider(knockbackResistance)
}

private class ConfigurableWearableOptions : ConfigAccess, WearableOptions {
    
    override val armorTypeProvider = getEntry<String>("armor_type").map { ArmorType.valueOf(it.uppercase()) }
    override val armorProvider = getOptionalEntry<Double>("armor").orElse(0.0)
    override val armorToughnessProvider = getOptionalEntry<Double>("armor_toughness").orElse(0.0)
    override val knockbackResistanceProvider = getOptionalEntry<Double>("knockback_resistance").orElse(0.0)
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}

private class SemiConfigurableWearableOptions : ConfigAccess, WearableOptions {
    
    override val armorTypeProvider: Provider<ArmorType>
    override val armorProvider = getOptionalEntry<Double>("armor").orElse(0.0)
    override val armorToughnessProvider = getOptionalEntry<Double>("armor_toughness").orElse(0.0)
    override val knockbackResistanceProvider = getOptionalEntry<Double>("knockback_resistance").orElse(0.0)
    
    constructor(armorType: ArmorType, path: String) : super(path) {
        this.armorTypeProvider = provider(armorType)
    }
    
    constructor(armorType: ArmorType, material: ItemNovaMaterial) : super(material) {
        this.armorTypeProvider = provider(armorType)
    }
    
}