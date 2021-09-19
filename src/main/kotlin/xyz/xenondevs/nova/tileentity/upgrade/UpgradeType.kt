package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.EFFICIENCY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.EFFICIENCY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ENERGY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ENERGY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.RANGE_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.RANGE_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPEED_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPEED_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TRANSLUCENT_EFFICIENCY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TRANSLUCENT_ENERGY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TRANSLUCENT_RANGE_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.TRANSLUCENT_SPEED_UPGRADE_ICON
import xyz.xenondevs.nova.util.data.getAllDoubles

enum class UpgradeType(val material: NovaMaterial, val icon: NovaMaterial, val grayIcon: NovaMaterial) {
    
    SPEED(SPEED_UPGRADE, SPEED_UPGRADE_ICON, TRANSLUCENT_SPEED_UPGRADE_ICON),
    EFFICIENCY(EFFICIENCY_UPGRADE, EFFICIENCY_UPGRADE_ICON, TRANSLUCENT_EFFICIENCY_UPGRADE_ICON),
    ENERGY(ENERGY_UPGRADE, ENERGY_UPGRADE_ICON, TRANSLUCENT_ENERGY_UPGRADE_ICON),
    RANGE(RANGE_UPGRADE, RANGE_UPGRADE_ICON, TRANSLUCENT_RANGE_UPGRADE_ICON);
    
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