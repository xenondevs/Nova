package xyz.xenondevs.nova.tileentity.network

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.World
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.serverTick
import java.util.concurrent.ConcurrentHashMap

private val PARALLEL_TICKING by MAIN_CONFIG.entry<Boolean>("network", "parallel_ticking")

internal sealed interface NetworkTicker {
    
    /**
     * A sequence of all [NetworkClusters][NetworkCluster] that are currently being ticked.
     */
    val clusters: Sequence<NetworkCluster>
    
    /**
     * A sequence of all [Networks][Network] that are currently being ticked.
     */
    val networks: Sequence<Network>
    
    /**
     * Replaces the networks to tick for [world] with [clusters]. Thread-safe.
     */
    fun submit(world: World, clusters: Iterable<NetworkCluster>)
    
    /**
     * Ticks all submitted [Networks][Network].
     * Should only be called from the main- or region thread.
     */
    fun tick()
    
    companion object {
        
        /**
         * Creates a new [NetworkTicker], depending on the server implementation.
         */
        fun create(): NetworkTicker {
            return when (ServerUtils.SERVER_SOFTWARE) {
                // ServerSoftware.FOLIA -> FoliaNetworkTicker()
                else -> PaperNetworkTicker()
            }
        }
        
    }
    
}

/**
 * A [NetworkTicker] implementation for server software that has a single main thread.
 */
private class PaperNetworkTicker : NetworkTicker {
    
    private val worlds = ConcurrentHashMap<World, Iterable<NetworkCluster>>()
    
    override val clusters: Sequence<NetworkCluster>
        get() = worlds.values.asSequence().flatten()
    
    override val networks: Sequence<Network>
        get() = clusters.flatMap { group -> group.networks }
    
    override fun submit(world: World, clusters: Iterable<NetworkCluster>) {
        worlds[world] = clusters
    }
    
    override fun tick() = runBlocking {
        val tick = serverTick
        for ((_, clusters) in worlds) {
            for (cluster in clusters) {
                if (PARALLEL_TICKING) {
                    launch { cluster.tickNetworks(tick) }
                } else {
                    cluster.tickNetworks(tick)
                }
            }
        }
    }
    
}