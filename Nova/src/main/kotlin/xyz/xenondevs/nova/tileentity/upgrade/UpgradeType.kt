package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.data.getAllDoubles

enum class UpgradeType(val material: NovaMaterial, val icon: NovaMaterial, val grayIcon: NovaMaterial) {
    
    SPEED(CoreItems.SPEED_UPGRADE, CoreGUIMaterial.SPEED_UPGRADE, CoreGUIMaterial.TP_SPEED_UPGRADE),
    EFFICIENCY(CoreItems.EFFICIENCY_UPGRADE, CoreGUIMaterial.EFFICIENCY_UPGRADE, CoreGUIMaterial.TP_EFFICIENCY_UPGRADE),
    ENERGY(CoreItems.ENERGY_UPGRADE, CoreGUIMaterial.ENERGY_UPGRADE, CoreGUIMaterial.TP_ENERGY_UPGRADE),
    FLUID(CoreItems.FLUID_UPGRADE, CoreGUIMaterial.FLUID_UPGRADE, CoreGUIMaterial.TP_FLUID_UPGRADE),
    RANGE(CoreItems.RANGE_UPGRADE, CoreGUIMaterial.RANGE_UPGRADE, CoreGUIMaterial.TP_RANGE_UPGRADE);
    
    private val modifierCache = HashMap<NovaMaterial, DoubleArray>()
    
    operator fun get(material: NovaMaterial): DoubleArray {
        return modifierCache.getOrPut(material) {
            val specificConfig = NovaConfig[material]
            return@getOrPut readConfiguredModifier(specificConfig) ?: readConfiguredModifier(DEFAULT_CONFIG)!!
        }
    }
    
    private fun readConfiguredModifier(config: NovaConfig): DoubleArray? =
        config.getArray("upgrade_modifiers.${name.lowercase()}")?.getAllDoubles()?.toDoubleArray()
    
    companion object {
        val ALL_ENERGY = arrayOf(SPEED, EFFICIENCY, ENERGY)
        val ENERGY_AND_RANGE = arrayOf(SPEED, EFFICIENCY, ENERGY, RANGE)
    }
    
}