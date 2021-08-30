package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType

class ProviderEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Int,
    private val defaultEnergyGeneration: Int,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    var energyGeneration = calculateEnergyGeneration()
    
    private fun calculateEnergyGeneration(): Int {
        upgradeHolder ?: return defaultEnergyGeneration
        
        return if (UpgradeType.SPEED in upgradeHolder.allowed) (defaultEnergyGeneration * upgradeHolder.getSpeedModifier()).toInt()
        else (defaultEnergyGeneration * upgradeHolder.getEfficiencyModifier()).toInt()
    }
    
    override fun handleUpgradesUpdate() {
        energyGeneration = calculateEnergyGeneration()
        
        super.handleUpgradesUpdate()
    }
    
}