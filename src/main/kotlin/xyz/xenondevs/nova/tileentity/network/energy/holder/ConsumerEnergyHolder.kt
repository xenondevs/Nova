package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder

class ConsumerEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Int,
    private val defaultEnergyConsumption: Int,
    private val defaultSpecialEnergyConsumption: Int,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    var energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
    var specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
    
    private fun calculateEnergyConsumption(default: Int): Int =
        (default * ((upgradeHolder?.getSpeedModifier() ?: 1.0)
            / (upgradeHolder?.getEfficiencyModifier() ?: 1.0))).toInt()
    
    override fun handleUpgradesUpdate() {
        energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
        specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
        
        super.handleUpgradesUpdate()
    }
    
}