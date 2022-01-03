package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType

class ProviderEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Long,
    private val defaultEnergyGeneration: Long,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    var energyGeneration = calculateEnergyGeneration()
    
    private fun calculateEnergyGeneration(): Long {
        upgradeHolder ?: return defaultEnergyGeneration
        
        return if (UpgradeType.SPEED in upgradeHolder.allowed) (defaultEnergyGeneration * upgradeHolder.getSpeedModifier()).toLong()
        else (defaultEnergyGeneration * upgradeHolder.getEfficiencyModifier()).toLong()
    }
    
    override fun handleUpgradesUpdate() {
        energyGeneration = calculateEnergyGeneration()
        
        super.handleUpgradesUpdate()
    }
    
}