package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.material.ItemNovaMaterial

@HardcodedMaterialOptions
fun ChargeableOptions(
    maxEnergy: Long
): ChargeableOptions = HardcodedChargeableOptions(maxEnergy)

sealed interface ChargeableOptions {
    
    val maxEnergy: Long
    
    companion object {
        
        fun configurable(material: ItemNovaMaterial): ChargeableOptions =
            ConfigurableChargeableOptions(material)
        
        fun configurable(path: String): ChargeableOptions =
            ConfigurableChargeableOptions(path)
        
    }
    
}

private class HardcodedChargeableOptions(
    override val maxEnergy: Long
) : ChargeableOptions

private class ConfigurableChargeableOptions : ConfigAccess, ChargeableOptions {
    
    override val maxEnergy by getEntry<Long>("max_energy")
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}