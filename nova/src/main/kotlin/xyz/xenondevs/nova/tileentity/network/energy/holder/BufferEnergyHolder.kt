package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeHolder
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeType
import java.util.*

fun BufferEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    infiniteEnergy: Boolean,
    upgradeHolder: UpgradeHolder,
    energyUpgradeType: UpgradeType<Double>,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) = BufferEnergyHolder(
    endPoint,
    defaultMaxEnergy,
    infiniteEnergy,
    upgradeHolder as UpgradeHolder?,
    energyUpgradeType as UpgradeType<Double>?,
    lazyDefaultConfig
)

class BufferEnergyHolder internal constructor(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Provider<Long>,
    val infiniteEnergy: Boolean,
    upgradeHolder: UpgradeHolder?,
    energyUpgradeType: UpgradeType<Double>?,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, energyUpgradeType, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.BUFFER
    
    override val requestedEnergy: Long
        get() = if (infiniteEnergy) Long.MAX_VALUE else maxEnergy - energy
    
    override var energy: Long
        get() = if (infiniteEnergy) Long.MAX_VALUE else super.energy
        set(value) {
            if (infiniteEnergy) return
            super.energy = value
        }
    
    constructor(
        endPoint: NetworkedTileEntity,
        defaultMaxEnergy: Provider<Long>,
        infiniteEnergy: Boolean,
        lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
    ) : this(
        endPoint,
        defaultMaxEnergy,
        infiniteEnergy,
        null, null,
        lazyDefaultConfig
    )
    
}