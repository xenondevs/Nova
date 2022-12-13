package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.provider.Provider
import xyz.xenondevs.nova.data.provider.provider
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

@Suppress("UNCHECKED_CAST", "USELESS_CAST") // compiler is confused otherwise
class ProviderEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    defaultEnergyGeneration: Provider<Long>?,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.EXTRACT
    
    private val defaultEnergyGeneration by (defaultEnergyGeneration ?: provider(0)) as Provider<Long>
    
    var energyGeneration = calculateEnergyGeneration()
        private set
    
    private fun calculateEnergyGeneration(): Long {
        upgradeHolder ?: return defaultEnergyGeneration
        
        return if (UpgradeType.SPEED in upgradeHolder.allowed) (defaultEnergyGeneration * upgradeHolder.getValue(UpgradeType.SPEED)).toLong()
        else (defaultEnergyGeneration * upgradeHolder.getValue(UpgradeType.EFFICIENCY)).toLong()
    }
    
    override fun reload() {
        energyGeneration = calculateEnergyGeneration()
        super.reload()
    }
    
}