package xyz.xenondevs.nova.world.block.tileentity.network.node

import org.bukkit.OfflinePlayer
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.format.NetworkState

/**
 * A node in a [Network].
 *
 * @see NetworkBridge
 * @see NetworkEndPoint
 */
sealed interface NetworkNode {
    
    /**
     * The [BlockPos] of this [NetworkNode]
     */
    val pos: BlockPos
    
    /**
     * The owner of this [NetworkNode] or null if it doesn't have one.
     */
    val owner: OfflinePlayer?
    
    /**
     * Whether this [NetworkNode] is valid and [Networks][Network] including it
     * should be ticked.
     */
    val isValid: Boolean
    
    /**
     * A set of [NetworkNodes][NetworkNode] that are not connected via networks,
     * but still share data and need to be clustered together, such as the left
     * and right part of a double chest.
     *
     * It is expected that all [linkedNodes] of linked nodes are equal.
     */
    val linkedNodes: Set<NetworkNode>
    
    /**
     * Called when a [NetworkNode] in one of the six cartesian directions is added, removed,
     * or updated in a way that affects this node.
     *
     * Also called after this node has been initialized.
     *
     * Note that a node may never receive an update when it is unloaded at that time,
     * so you shouldn't rely on this method for important updates, it is rather
     * intended to update the visual state and similar non-critical things.
     */
    suspend fun handleNetworkUpdate(state: NetworkState) = Unit
    
    /**
     * Called when a [NetworkNode] is loaded into the [state].
     *
     * Note that this function is only called when a [NetworkNode] is loaded,
     * not when the node was added using [NetworkManager.queueAddEndPoint] or
     * [NetworkManager.queueAddBridge].
     */
    suspend fun handleNetworkLoaded(state: NetworkState) = Unit
    
}