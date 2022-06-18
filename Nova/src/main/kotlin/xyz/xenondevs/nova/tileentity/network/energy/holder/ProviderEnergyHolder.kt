package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.data.config.notReloadable
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

@Suppress("UNCHECKED_CAST", "USELESS_CAST") // compiler is confused otherwise
class ProviderEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: ValueReloadable<Long>,
    defaultEnergyGeneration: ValueReloadable<Long>?,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.EXTRACT
    
    private val defaultEnergyGeneration by (defaultEnergyGeneration ?: notReloadable(0)) as ValueReloadable<Long>
    
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