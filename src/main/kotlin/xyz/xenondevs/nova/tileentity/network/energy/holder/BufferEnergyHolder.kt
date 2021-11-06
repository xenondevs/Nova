package xyz.xenondevs.nova.tileentity.network.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType

class BufferEnergyHolder(
    endPoint: NetworkedTileEntity,
    defaultMaxEnergy: Long,
    private val creative: Boolean,
    lazyDefaultConfig: () -> MutableMap<BlockFace, EnergyConnectionType>
) : EnergyHolder(endPoint, defaultMaxEnergy, null, lazyDefaultConfig) {
    
    override val requestedEnergy: Long
        get() = if (creative) Long.MAX_VALUE else maxEnergy - energy
    
    override var energy: Long
        get() = if (creative) Long.MAX_VALUE else super.energy
        set(value) {
            if (creative) return
            super.energy = value
        }
    
}