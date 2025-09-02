package xyz.xenondevs.nova.world.block.tileentity.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.World
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
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
    val networks: Sequence<Network<*>>
    
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
    
    override val networks: Sequence<Network<*>>
        get() = clusters.flatMap { cluster -> cluster.networks }
    
    override fun submit(world: World, clusters: Iterable<NetworkCluster>) {
        worlds[world] = clusters
    }
    
    override fun tick() = runBlocking {
        val tick = serverTick
        
        if (PARALLEL_TICKING) {
            tickParallel(tick)
        } else {
            tickSequential(tick)
        }
    }
    
    private fun tickSequential(tick: Int) {
        clusters.forEach { it.preTickSync(tick) }
        clusters.forEach { cluster ->
            cluster.preTick(tick)
            cluster.tick(tick)
            cluster.postTick(tick)
        }
        clusters.forEach { it.postTickSync(tick) }
    }
    
    private suspend fun tickParallel(tick: Int) {
        clusters.forEach { it.preTickSync(tick) }
        coroutineScope {
            clusters.forEach { cluster ->
                launch(Dispatchers.Default) {
                    cluster.preTick(tick)
                    cluster.tick(tick)
                    cluster.postTick(tick)
                }
            }
        }
        clusters.forEach { it.postTickSync(tick) }
    }
    
}