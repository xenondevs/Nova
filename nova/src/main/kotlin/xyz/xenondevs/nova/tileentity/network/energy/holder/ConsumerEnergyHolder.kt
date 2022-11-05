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
class ConsumerEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    defaultEnergyConsumption: Provider<Long>?,
    defaultSpecialEnergyConsumption: Provider<Long>?,
    upgradeHolder: UpgradeHolder?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.INSERT
    
    private val defaultEnergyConsumption by (defaultEnergyConsumption ?: provider(0)) as Provider<Long>
    private val defaultSpecialEnergyConsumption by (defaultSpecialEnergyConsumption ?: provider(0L)) as Provider<Long>
    
    var energyConsumption = calculateEnergyConsumption(this.defaultEnergyConsumption)
        private set
    var specialEnergyConsumption = calculateEnergyConsumption(this.defaultSpecialEnergyConsumption)
        private set
    
    private fun calculateEnergyConsumption(default: Long): Long =
        (default * ((upgradeHolder?.getValue(UpgradeType.SPEED) ?: 1.0)
            / (upgradeHolder?.getValue(UpgradeType.EFFICIENCY) ?: 1.0))).toLong()
    
    override fun reload() {
        energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
        specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
        
        super.reload()
    }
    
}