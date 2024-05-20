package xyz.xenondevs.nova.tileentity.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withLock
import org.bukkit.World
import xyz.xenondevs.commons.collections.mapToBooleanArray
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.network.task.LoadChunkTask
import xyz.xenondevs.nova.tileentity.network.task.NetworkTask
import xyz.xenondevs.nova.tileentity.network.task.ProtectedNodeNetworkTask
import xyz.xenondevs.nova.tileentity.network.task.ProtectionResult
import xyz.xenondevs.nova.tileentity.network.task.UnloadChunkTask
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import kotlin.time.measureTime

internal class NetworkConfigurator(private val world: World, private val ticker: NetworkTicker) {
    
    /**
     * The channel responsible for processing [NetworkTasks][NetworkTask].
     */
    private val channel = Channel<NetworkTask>(capacity = UNLIMITED)
    
    /**
     * Stores protection query results for [ProtectedNodeNetworkTasks][ProtectedNodeNetworkTask].
     */
    private val protectionResults = ConcurrentHashMap<ProtectedNodeNetworkTask, Deferred<ProtectionResult>>()
    
    /**
     * The current network state, i.e. which nodes are connected to which networks.
     */
    val state = WorldDataManager.getWorldStorage(world).networkState
    
    init {
        CoroutineScope(NetworkManager.SUPERVISOR).launch(CoroutineName("Network configurator ${world.name}")) {
            channel.consumeEach {
                try {
                    val time = measureTime { processTask(it) }
                    if (it !is LoadChunkTask && it !is UnloadChunkTask)
                        LOGGER.info("Executed NetworkTask: $it in $time")
                } catch (e: Exception) {
                    LOGGER.log(Level.SEVERE, "An exception occurred trying to process NetworkTask: $it", e)
                }
            }
        }
    }
    
    /**
     * Enqueues the [task] to be processed by the appropriate coroutine.
     * Also queries protection in case of [ProtectedNodeNetworkTask].
     */
    fun queueTask(task: NetworkTask) = runBlocking {
        if (task is ProtectedNodeNetworkTask)
            protectionResults[task] = async(Dispatchers.Default) { queryProtection(task.node) }
        
        channel.send(task)
    }
    
    /**
     * Queries the block use protection in all 6 cartesian directions around [node] asynchronously
     * and returns the [ProtectionResult].
     */
    private suspend fun queryProtection(node: NetworkNode): ProtectionResult = coroutineScope {
        val owner = node.owner
        if (owner != null) {
            CUBE_FACES
                .map { face -> async { ProtectionManager.canUseBlock(owner, null, node.pos.advance(face, 1)) } }
                .mapToBooleanArray { it.await() }
                .let(::ProtectionResult)
        } else ProtectionResult.ALL_ALLOWED
    }
    
    /**
     * Processes a queued [NetworkNode] task, then builds dirty [ProtoNetworks][ProtoNetwork], and
     * submits the [NetworkClusters][NetworkCluster] to the [ticker].
     */
    private suspend fun processTask(task: NetworkTask) {
        state.mutex.withLock {
            // await protection results, run task
            if (task is ProtectedNodeNetworkTask) {
                task.result = protectionResults.remove(task)?.await()
                    ?: throw IllegalStateException("Protection was not queried")
            }
            val hasWritten = task.run()
            if (!hasWritten)
                return
            
            ticker.submit(world, buildClusters())
        }
    }
    
    private suspend fun buildClusters(): List<NetworkCluster> = coroutineScope {
        // collect proto clusters
        val protoClusters = state.networks
            .mapTo(HashSet()) { it.cluster ?: throw IllegalStateException("Cluster for $it is uninitialized") }
        // debug: verify proto clusters
        if (IS_DEV_SERVER)
            verifyClusters(protoClusters)
        
        // build clusters
        return@coroutineScope protoClusters
            .map { cluster ->
                if (cluster.dirty) {
                    async { buildDirtyCluster(cluster) }
                } else {
                    CompletableDeferred(cluster.cluster)
                }
            }.awaitAll()
    }
    
    private fun buildDirtyCluster(protoCluster: ProtoNetworkCluster): NetworkCluster {
        val networks = ArrayList<Network<*>>()
        for (protoNetwork in protoCluster) {
            networks += if (protoNetwork.dirty) buildDirtyNetwork(protoNetwork) else protoNetwork.network
        }
        
        val cluster = NetworkCluster(protoCluster.uuid, networks)
        protoCluster.cluster = cluster
        protoCluster.dirty = false
        
        return cluster
    }
    
    private fun <T : Network<T>> buildDirtyNetwork(protoNetwork: ProtoNetwork<T>): Network<T> {
        val data = protoNetwork.immutableCopy()
        val network = protoNetwork.type.create(data)
        protoNetwork.network = network
        protoNetwork.markClean()
        return network
    }
    
    private fun verifyClusters(protoClusters: Set<ProtoNetworkCluster>) {
        val networks = state.networks.toHashSet()
        for (cluster in protoClusters) {
            for (network in cluster) {
                check(network in networks) { "Cluster $cluster contains unregistered network $network" }
            }
        }
    }
    
}