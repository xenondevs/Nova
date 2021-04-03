package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace

interface EnergyProvider : NetworkEndPoint {
    
    /**
     * Faces that are allowed to provide energy and to
     * which [EnergyNetwork] they're currently connected.
     * 
     * The [EnergyNetwork] can be null, not all cubical faces
     * must exist in this map.
     * 
     * Faces other than the 6 cubical faces (e.g. [BlockFace.NORTH_WEST]...) are ignored.
     */
    val provideNetworks: MutableMap<BlockFace, EnergyNetwork?>
    
    /**
     * The amount of energy that this [EnergyProvider] provides to the network.
     */
    val providedEnergyAmount: Int
    
    /**
     * Called to notify the [EnergyProvider] that a specific amount of energy has
     * been taken from it.
     */
    fun takeEnergy(energyAmount: Int)
    
}