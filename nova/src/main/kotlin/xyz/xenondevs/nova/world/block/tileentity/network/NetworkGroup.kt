package xyz.xenondevs.nova.world.block.tileentity.network

import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType

/**
 * The data of a network group, containing all [networks] as well as the [type] of all networks.
 */
interface NetworkGroupData<T : Network<T>> {
    val type: NetworkType<T>
    val networks: List<T>
}

internal class ImmutableNetworkGroupData<T : Network<T>>(
    override val type: NetworkType<T>,
    override val networks: List<T>
) : NetworkGroupData<T>

/**
 * A collection of [Networks][Network] of the same [NetworkType] from
 * the same [NetworkCluster], handling ticking logic for these networks.
 *
 * Independent network groups may be ticked in parallel!
 * Because of that, the functions not suffixed with `Sync` may not interact with any world state
 * outside of the blocks that are in this network.
 * This includes not causing block updates, changing vanilla block states, or firing bukkit events.
 */
interface NetworkGroup<T : Network<T>> : NetworkGroupData<T> {
    
    /**
     * Called every [NetworkType.tickDelay] ticks before any network group is ticked,
     * on the main thread.
     */
    fun preTickSync() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks before any network group in this
     * network group's [NetworkCluster] is ticked, in parallel with network groups from
     * other clusters.
     */
    fun preTick() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks, in parallel with network groups
     * from other clusters.
     */
    fun tick()
    
    /**
     * Called every [NetworkType.tickDelay] ticks after all network groups in this
     * network group's [NetworkCluster] have been ticked, in parallel with networks
     * groups from other clusters.
     */
    fun postTick() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks after all networks groups have been
     * ticked, on the main thread.
     */
    fun postTickSync() = Unit
    
}