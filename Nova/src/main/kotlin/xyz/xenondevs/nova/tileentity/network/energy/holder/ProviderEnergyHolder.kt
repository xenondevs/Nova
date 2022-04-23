package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

class ProviderEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Long,
    private val defaultEnergyGeneration: Long,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.EXTRACT
    
    var energyGeneration = calculateEnergyGeneration()
    
    private fun calculateEnergyGeneration(): Long {
        upgradeHolder ?: return defaultEnergyGeneration
        
        return if (UpgradeType.SPEED in upgradeHolder.allowed) (defaultEnergyGeneration * upgradeHolder.getValue(UpgradeType.SPEED)).toLong()
        else (defaultEnergyGeneration * upgradeHolder.getValue(UpgradeType.EFFICIENCY)).toLong()
    }
    
    override fun handleUpgradesUpdate() {
        energyGeneration = calculateEnergyGeneration()
        
        super.handleUpgradesUpdate()
    }
    
}