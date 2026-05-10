package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType

/**
 * A type of [NetworkNode] that contains data to be modified during the network tick.
 *
 * Types that inherit from both [NetworkEndPoint] and [NetworkBridge] are not allowed.
 *
 * @see NetworkBridge
 */
interface NetworkEndPoint : NetworkNode {
    
    /**
     * The [EndPointDataHolders][EndPointDataHolder] that contain the data of this [NetworkEndPoint].
     */
    val holders: Collection<EndPointDataHolder>
    
    /**
     * Whether this [NetworkEndPoint] requests the formation of a local network at [face] between it
     * and another end point. Local networks are networks that only consist of two end points
     * with no [NetworkBridges][NetworkBridge] inbetween.
     * 
     * A local network will only be created if at least one end point actually requests a local network.
     * 
     * The idea behind this is that there are certain types of local networks (such as chest-chest) that
     * are nonsensical (if both chests are set to [NetworkConnectionType.BUFFER]).
     * On the other hand, other tile-entities may behave differently on [NetworkConnectionType.BUFFER] mode,
     * only allowing certain slots to be extracted or inserted to, in which case such configurations would make sense.
     * 
     * This function is checked in addition to [NetworkType.validateLocal].
     * In order for a network to be created, the type's validator must pass and at least one
     * end point must request a local network.
     * 
     * Called on the network configurator thread (off-main).
     */
    fun requestsLocalNetwork(face: BlockFace): Boolean = true
    
}