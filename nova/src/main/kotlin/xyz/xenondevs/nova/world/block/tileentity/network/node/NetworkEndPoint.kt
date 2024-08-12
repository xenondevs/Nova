package xyz.xenondevs.nova.world.block.tileentity.network.node

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
    val holders: Set<EndPointDataHolder>
    
}