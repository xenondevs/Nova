package xyz.xenondevs.nova.tileentity.network

import xyz.xenondevs.nova.tileentity.network.type.NetworkType

interface NetworkGroupData<T : Network<T>> {
    val type: NetworkType<T>
    val networks: List<T>
}

internal class ImmutableNetworkGroupData<T : Network<T>>(
    override val type: NetworkType<T>,
    override val networks: List<T>
) : NetworkGroupData<T>

interface NetworkGroup<T : Network<T>> : NetworkGroupData<T> {
    
    /**
     * Called every [NetworkType.tickDelay] ticks before any network is ticked,
     * on the main thread.
     */
    fun preTickSync() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks before any network in this
     * network's [NetworkCluster] is ticked, in parallel with networks from
     * other clusters.
     */
    fun preTick() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks, in parallel with networks
     * from other clusters.
     */
    fun tick()
    
    /**
     * Called every [NetworkType.tickDelay] ticks after all networks in this
     * network's [NetworkCluster] have been ticked, in parallel with networks
     * from other clusters.
     */
    fun postTick() = Unit
    
    /**
     * Called every [NetworkType.tickDelay] ticks after all networks have been ticked,
     * on the main thread.
     */
    fun postTickSync() = Unit
    
}