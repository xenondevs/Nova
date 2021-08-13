package xyz.xenondevs.nova.tileentity.network.energy

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint

interface EnergyStorage : NetworkEndPoint {
    
    val energyConfig: MutableMap<BlockFace, EnergyConnectionType>
    val providedEnergy: Int
    val requestedEnergy: Int
    
    fun addEnergy(energy: Int)
    
    fun removeEnergy(energy: Int)
    
}

enum class EnergyConnectionType {
    
    NONE,
    PROVIDE,
    CONSUME,
    BUFFER
    
}