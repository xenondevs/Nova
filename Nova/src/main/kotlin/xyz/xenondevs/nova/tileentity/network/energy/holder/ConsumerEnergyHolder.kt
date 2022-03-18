package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import java.util.*

class ConsumerEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Long,
    private val defaultEnergyConsumption: Long,
    private val defaultSpecialEnergyConsumption: Long,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, EnergyConnectionType>
) : EnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    var energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
    var specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
    
    private fun calculateEnergyConsumption(default: Long): Long =
        (default * ((upgradeHolder?.getSpeedModifier() ?: 1.0)
            / (upgradeHolder?.getEfficiencyModifier() ?: 1.0))).toLong()
    
    override fun handleUpgradesUpdate() {
        energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
        specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
        
        super.handleUpgradesUpdate()
    }
    
}