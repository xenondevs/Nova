package xyz.xenondevs.nova.tileentity.network.type.energy.holder

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.enumSet
import xyz.xenondevs.nova.tileentity.network.node.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType

/**
 * End point data holder for nova:energy networks.
 */
interface EnergyHolder : EndPointDataHolder {
    
    val allowedConnectionType: NetworkConnectionType
    
    /**
     * Stores which [NetworkConnectionType] is used for each [BlockFace].
     */
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
    
    /**
     * The current amount of energy in this [EnergyHolder].
     */
    var energy: Long
    
    /**
     * The requested amount of energy that networks should try to insert into this [EnergyHolder].
     */
    val requestedEnergy: Long
    
    override val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(enumSet()) { (face, type) ->
            if (type != NetworkConnectionType.NONE) face else null
        }
    
}