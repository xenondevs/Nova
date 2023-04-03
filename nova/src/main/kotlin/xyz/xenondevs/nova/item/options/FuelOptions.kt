package xyz.xenondevs.nova.item.options

import xyz.xenondevs.nova.data.config.ConfigAccess
import xyz.xenondevs.nova.item.NovaItem

@HardcodedMaterialOptions
fun FuelOptions(
    burnTime: Int,
): FuelOptions = HardcodedFuelOptions(burnTime)

sealed interface FuelOptions {
    
    /**
     * The burn time of this fuel, in ticks.
     */
    val burnTime: Int
    
    companion object {
        
        fun configurable(item: NovaItem): FuelOptions =
            ConfigurableFuelOptions(item)
        
        fun configurable(path: String): FuelOptions =
            ConfigurableFuelOptions(path)
        
    }
    
}

private class HardcodedFuelOptions(
    override val burnTime: Int
) : FuelOptions

private class ConfigurableFuelOptions : ConfigAccess, FuelOptions {
    
    override val burnTime by getEntry<Int>("burn_time")
    
    constructor(path: String) : super(path)
    constructor(item: NovaItem) : super(item)
    
}