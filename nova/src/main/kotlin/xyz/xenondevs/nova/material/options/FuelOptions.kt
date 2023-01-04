package xyz.xenondevs.nova.material.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.material.ItemNovaMaterial

@HardcodedMaterialOptions
fun FuelOptions(
    burnTime: Int,
): FuelOptions = HardcodedFuelOptions(burnTime)

sealed interface FuelOptions {
    
    /**
     * The burn time of this fuel, in ticks.
     */
    val burnTime: Int
    
    companion object : MaterialOptionsType<FuelOptions> {
        
        override fun configurable(material: ItemNovaMaterial): FuelOptions =
            ConfigurableFuelOptions(material)
        
        override fun configurable(path: String): FuelOptions =
            ConfigurableFuelOptions(path)
        
    }
    
}

private class HardcodedFuelOptions(
    override val burnTime: Int
) : FuelOptions

private class ConfigurableFuelOptions : ConfigAccess, FuelOptions {
    
    override val burnTime by getEntry<Int>("burn_time")
    
    constructor(path: String) : super(path)
    constructor(material: ItemNovaMaterial) : super(material)
    
}