package xyz.xenondevs.nova.world.block.tileentity.network

import org.bukkit.World
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.serverTick
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.Phaser

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
     * Queues a replacement of the networks to tick for [world] with [clusters]. Thread-safe.
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

private data class ClusterSubmission(val world: World, val clusters: Iterable<NetworkCluster>)

/**
 * A [NetworkTicker] implementation for server software that has a single main thread.
 */
private class PaperNetworkTicker : NetworkTicker {
    
    private val worlds = HashMap<World, Iterable<NetworkCluster>>()
    private val pendingSubmissions = ConcurrentLinkedQueue<ClusterSubmission>()
    private val executor = Executors.newWorkStealingPool()
    
    override val clusters: Sequence<NetworkCluster>
        get() = worlds.values.asSequence().flatten()
    
    override val networks: Sequence<Network<*>>
        get() = clusters.flatMap { cluster -> cluster.networks }
    
    override fun submit(world: World, clusters: Iterable<NetworkCluster>) {
        pendingSubmissions += ClusterSubmission(world, clusters)
    }
    
    override fun tick() {
        // apply pending submissions
        while (true) {
            val submission = pendingSubmissions.poll() ?: break
            worlds[submission.world] = submission.clusters
        }
        
        val tick = serverTick
        
        if (PARALLEL_TICKING) {
            tickParallel(tick)
        } else {
            tickSequential(tick)
        }
    }
    
    private fun tickSequential(tick: Int) {
        clusters.forEach {
            it.updateIsValid()
            it.preTickSync(tick)
        }
        clusters.forEach { cluster ->
            cluster.preTick(tick)
            cluster.tick(tick)
            cluster.postTick(tick)
        }
        clusters.forEach { it.postTickSync(tick) }
    }
    
    private fun tickParallel(tick: Int) {
        val completion = Phaser(1)
        clusters.forEach { cluster ->
            completion.register()
            executor.execute {
                try {
                    cluster.updateIsValid()
                } finally {
                    completion.arriveAndDeregister()
                }
            }
        }
        completion.arriveAndAwaitAdvance()
        
        clusters.forEach { it.preTickSync(tick) }
        
        clusters.forEach { cluster ->
            completion.register()
            executor.execute {
                try {
                    cluster.preTick(tick)
                    cluster.tick(tick)
                    cluster.postTick(tick)
                } catch (t: Throwable) {
                    LOGGER.error("An exception occurred while ticking a network cluster", t)
                } finally {
                    completion.arriveAndDeregister()
                }
            }
        }
        completion.arriveAndAwaitAdvance()
        clusters.forEach { it.postTickSync(tick) }
    }
    
}