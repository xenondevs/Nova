package xyz.xenondevs.nova.item.options

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.provider
import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.player.equipment.ArmorType

@HardcodedMaterialOptions
fun WearableOptions(
    armorType: ArmorType,
    armor: Double = 0.0,
    armorToughness: Double = 0.0,
    knockbackResistance: Double = 0.0,
    equipSound: String? = null
): WearableOptions = HardcodedWearableOptions(armorType, armor, armorToughness, knockbackResistance, equipSound)

sealed interface WearableOptions {
    
    val armorTypeProvider: Provider<ArmorType>
    val armorProvider: Provider<Double>
    val armorToughnessProvider: Provider<Double>
    val knockbackResistanceProvider: Provider<Double>
    val equipSoundProvider: Provider<String?>
    
    val armorType: ArmorType
        get() = armorTypeProvider.value
    val armor: Double
        get() = armorProvider.value
    val armorToughness: Double
        get() = armorToughnessProvider.value
    val knockbackResistance: Double
        get() = knockbackResistanceProvider.value
    val equipSound: String?
        get() = equipSoundProvider.value
    
    companion object {
        
        fun configurable(armorType: ArmorType, equipSound: String?, item: NovaItem): WearableOptions =
            ConfigurableWearableOptions(armorType, equipSound, item)
        
        fun configurable(armorType: ArmorType, equipSound: String?, path: String): WearableOptions =
            ConfigurableWearableOptions(armorType, equipSound, path)
        
    }
    
}

private class HardcodedWearableOptions(
    armorType: ArmorType,
    armor: Double,
    armorToughness: Double,
    knockbackResistance: Double,
    equipSound: String?
) : WearableOptions {
    override val armorTypeProvider = provider(armorType)
    override val armorProvider = provider(armor)
    override val armorToughnessProvider = provider(armorToughness)
    override val knockbackResistanceProvider = provider(knockbackResistance)
    override val equipSoundProvider = provider(equipSound)
}

private class ConfigurableWearableOptions : ConfigAccess, WearableOptions {
    
    override val equipSoundProvider: Provider<String?>
    override val armorTypeProvider: Provider<ArmorType>
    override val armorProvider = getOptionalEntry<Double>("armor").orElse(0.0)
    override val armorToughnessProvider = getOptionalEntry<Double>("armor_toughness").orElse(0.0)
    override val knockbackResistanceProvider = getOptionalEntry<Double>("knockback_resistance").orElse(0.0)
    
    constructor(armorType: ArmorType, soundEvent: String?, path: String) : super(path) {
        this.armorTypeProvider = provider(armorType)
        this.equipSoundProvider = provider(soundEvent)
    }
    
    constructor(armorType: ArmorType, soundEvent: String?, item: NovaItem) : super(item) {
        this.armorTypeProvider = provider(armorType)
        this.equipSoundProvider = provider(soundEvent)
    }
    
}