package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.ValueReloadable
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import java.util.*

class BufferEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: ValueReloadable<Long>,
    val creative: Boolean,
    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
) : NovaEnergyHolder(endPoint, defaultMaxEnergy, null, lazyDefaultConfig) {
    
    override val allowedConnectionType = NetworkConnectionType.BUFFER
    
    override val requestedEnergy: Long
        get() = if (creative) Long.MAX_VALUE else maxEnergy - energy
    
    override var energy: Long
        get() = if (creative) Long.MAX_VALUE else super.energy
        set(value) {
            if (creative) return
            super.energy = value
        }
    
}