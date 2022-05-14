package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeTypeRegistry.register
import xyz.xenondevs.nova.util.data.getListOrNull

class UpgradeType<T> internal constructor(
    val id: NamespacedId,
    val item: ItemNovaMaterial,
    val icon: ItemNovaMaterial,
    private val configLoader: (Any) -> T
) {
    
    private val modifierCache = HashMap<ItemNovaMaterial, List<T>>()
    private val defaultConfig by configReloadable { NovaConfig["${id.namespace}:upgrade_values"] }
    
    fun getValue(material: ItemNovaMaterial, level: Int): T {
        val values = getUpgradeValues(material)
        return values[level.coerceIn(0..values.lastIndex)]
    }
    
    fun getUpgradeValues(material: ItemNovaMaterial): List<T> {
        return modifierCache.getOrPut(material) {
            val specificConfig = NovaConfig[material]
            
            val configValues: List<Any>? = specificConfig.getListOrNull("upgrade_values.${id.name}")
                ?: defaultConfig.getListOrNull(id.name)
            checkNotNull(configValues) { "No upgrade values present for $id" }
            
            return@getOrPut configValues.map(configLoader)
        }
    }
    
    companion object {
        val SPEED = register<Double>("speed", CoreItems.SPEED_UPGRADE, CoreGUIMaterial.SPEED_UPGRADE)
        val EFFICIENCY = register<Double>("efficiency", CoreItems.EFFICIENCY_UPGRADE, CoreGUIMaterial.EFFICIENCY_UPGRADE)
        val ENERGY = register<Double>("energy", CoreItems.ENERGY_UPGRADE, CoreGUIMaterial.ENERGY_UPGRADE)
        val FLUID = register<Double>("fluid", CoreItems.FLUID_UPGRADE, CoreGUIMaterial.FLUID_UPGRADE)
        val RANGE = register<Int>("range", CoreItems.RANGE_UPGRADE, CoreGUIMaterial.RANGE_UPGRADE)
        
        internal fun init() = Unit
    }
    
    
}