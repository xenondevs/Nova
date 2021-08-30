package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry.EFFICIENCY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.EFFICIENCY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ENERGY_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.ENERGY_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.RANGE_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.RANGE_UPGRADE_ICON
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPEED_UPGRADE
import xyz.xenondevs.nova.material.NovaMaterialRegistry.SPEED_UPGRADE_ICON
import kotlin.math.pow

// TODO nerf
private val DEFAULT_MODIFIERS = (0..10).map { if (it == 0) 1.0 else if (it == 1) it + 0.25 else it - 0.25 }.toDoubleArray()
private val SPEED_MODIFIERS = (0..10).map { it.toDouble().pow(0.95) + 1 }.toDoubleArray()
private val RANGE_MODIFIERS = (0..10).map { it.toDouble() }.toDoubleArray()

enum class UpgradeType(val material: NovaMaterial, val icon: NovaMaterial, val modifiers: DoubleArray) {
    
    SPEED(SPEED_UPGRADE, SPEED_UPGRADE_ICON, SPEED_MODIFIERS),
    EFFICIENCY(EFFICIENCY_UPGRADE, EFFICIENCY_UPGRADE_ICON, DEFAULT_MODIFIERS),
    ENERGY(ENERGY_UPGRADE, ENERGY_UPGRADE_ICON, DEFAULT_MODIFIERS),
    RANGE(RANGE_UPGRADE, RANGE_UPGRADE_ICON, RANGE_MODIFIERS);
    
    companion object {
        val ALL_ENERGY = arrayOf(SPEED, EFFICIENCY, ENERGY)
        val ENERGY_AND_RANGE = arrayOf(SPEED, EFFICIENCY, ENERGY, RANGE)
    }
    
    
}