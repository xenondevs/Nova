package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace

interface EnergyStorage : EnergyNode {
    
    val networks: MutableMap<BlockFace, EnergyNetwork>
    val configuration: Map<BlockFace, EnergyConnectionType>
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