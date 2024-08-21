package xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.world.block.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType

/**
 * End point data holder for nova:energy networks.
 */
interface EnergyHolder : EndPointDataHolder {
    
    val allowedConnectionType: NetworkConnectionType
    
    /**
     * The [BlockFaces][BlockFace] that can never have a connection.
     */
    val blockedFaces: Set<BlockFace>
    
    /**
     * Stores which [NetworkConnectionType] is used for each [BlockFace].
     */
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
    
    /**
     * The current amount of energy in this [EnergyHolder].
     */
    var energy: Long
    
    /**
     * The maximum amount of energy this [EnergyHolder] can store.
     */
    val maxEnergy: Long
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
}