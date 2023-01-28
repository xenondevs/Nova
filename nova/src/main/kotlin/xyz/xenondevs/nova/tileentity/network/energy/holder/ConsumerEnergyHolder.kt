package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.NullProvider
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

fun ConsumerEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    defaultEnergyConsumption: Provider<Long>? = null,
    defaultSpecialEnergyConsumption: Provider<Long>? = null,
    upgradeHolder: UpgradeHolder,
    speedUpgradeType: UpgradeType<Double>,
    efficiencyUpgradeType: UpgradeType<Double>,
    energyUpgradeType: UpgradeType<Double>,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) = ConsumerEnergyHolder(
    endPoint,
    defaultMaxEnergy,
    defaultEnergyConsumption,
    defaultSpecialEnergyConsumption,
    upgradeHolder as UpgradeHolder?,
    speedUpgradeType as UpgradeType<Double>?,
    efficiencyUpgradeType as UpgradeType<Double>?,
    energyUpgradeType as UpgradeType<Double>?,
    lazyDefaultConfig
)

@Suppress("UNCHECKED_CAST", "USELESS_CAST") // compiler is confused otherwise
class ConsumerEnergyHolder internal constructor(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    defaultEnergyConsumption: Provider<Long>?,
    defaultSpecialEnergyConsumption: Provider<Long>?,
    upgradeHolder: UpgradeHolder?,
    private val speedUpgradeType: UpgradeType<Double>?,
    private val efficiencyUpgradeType: UpgradeType<Double>?,
    energyUpgradeType: UpgradeType<Double>?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, energyUpgradeType, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.INSERT
    
    private val defaultEnergyConsumption by (defaultEnergyConsumption ?: NullProvider) as Provider<Long?>
    private val defaultSpecialEnergyConsumption by (defaultSpecialEnergyConsumption ?: NullProvider) as Provider<Long?>
    
    constructor(
        endPoint: NetworkedTileEntity,
        defaultMaxEnergy: Provider<Long>,
        defaultEnergyConsumption: Provider<Long>? = null,
        defaultSpecialEnergyConsumption: Provider<Long>? = null,
        lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
    ) : this(
        endPoint,
        defaultMaxEnergy,
        defaultEnergyConsumption,
        defaultSpecialEnergyConsumption,
        null, null, null, null,
        lazyDefaultConfig
    )
    
    var energyConsumption = calculateEnergyConsumption(this.defaultEnergyConsumption)
        private set
    var specialEnergyConsumption = calculateEnergyConsumption(this.defaultSpecialEnergyConsumption)
        private set
    
    private fun calculateEnergyConsumption(default: Long?): Long {
        if (default == null)
            return 0L
        
        if (upgradeHolder != null && speedUpgradeType != null && efficiencyUpgradeType != null) {
            return (default * (upgradeHolder.getValue(speedUpgradeType) / upgradeHolder.getValue(efficiencyUpgradeType))).toLong()
        }
        
        return default
    }
    
    override fun reload() {
        energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
        specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
        
        super.reload()
    }
    
}