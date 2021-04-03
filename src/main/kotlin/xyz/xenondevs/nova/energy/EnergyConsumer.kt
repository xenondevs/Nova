package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace

interface EnergyConsumer : NetworkEndPoint {
    
    /**
     * Faces that are allowed to consume energy and to
     * which [EnergyNetwork] they're currently connected.
     *
     * The [EnergyNetwork] can be null, not all cubical faces
     * must exist in this map.
     *
     * Faces other than the 6 cubical faces (e.g. [BlockFace.NORTH_WEST]...) are ignored.
     */
    val consumeNetworks: MutableMap<BlockFace, EnergyNetwork?>
    
    /**
     * The amount of energy requested by this [EnergyConsumer].
     */
    val requestedEnergyAmount: Int
    
    /**
     * Called to notify the [EnergyConsumer] that a specific amount
     * of energy has been given to it.
     */
    fun consumeEnergy(energyAmount: Int)
    
}