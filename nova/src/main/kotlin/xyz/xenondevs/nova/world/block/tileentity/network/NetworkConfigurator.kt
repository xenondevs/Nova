package xyz.xenondevs.nova.world.block.tileentity.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.World
import xyz.xenondevs.commons.collections.mapToBooleanArray
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.task.LoadChunkTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.NetworkTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.ProtectedNodeNetworkTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.ProtectionResult
import xyz.xenondevs.nova.world.block.tileentity.network.task.UnloadChunkTask
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

internal class NetworkConfigurator(private val world: World, private val ticker: NetworkTicker) {
    
    /**
     * Lock for [loadedChunks] and [taskBacklog].
     */
    private val queueLock = Mutex()
    
    /**
     * Contains all positions of chunks that will be loaded for a network task queued now.
     * (Meaning that a chunk is not necessarily loaded now, but a load is queued, or that
     * a chunk is loaded now, but an unload is queued.)
     */
    private val loadedChunks = HashSet<ChunkPos>()
    
    /**
     * Contains tasks for chunks that are not loaded yet.
     */
    private val taskBacklog = HashMap<ChunkPos, ArrayList<NetworkTask>>()
    
    /**
     * Stores protection query results for [ProtectedNodeNetworkTasks][ProtectedNodeNetworkTask].
     */
    private val protectionResults = ConcurrentHashMap<ProtectedNodeNetworkTask, Deferred<ProtectionResult>>()
    
    /**
     * The current network state, i.e. which nodes are connected to which networks.
     */
    val state = WorldDataManager.getWorldStorage(world).networkState
    
    /**
     * The channel responsible for processing [NetworkTasks][NetworkTask].
     */
    private val channel = Channel<NetworkTask>(capacity = UNLIMITED)
    
    /**
     * The coroutine responsible for processing [NetworkTasks][NetworkTask].
     */
    private val job = CoroutineScope(NetworkManager.SUPERVISOR).launch(CoroutineName("Network configurator ${world.name}")) {
        channel.consumeEach { task ->
            try {
                task.event.begin()
                processTask(task)
                task.event.commit()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to process NetworkTask: $task", e)
            }
        }
    }
    
    /**
     * Supervisor job for protection queries.
     */
    private val protectionSupervisor = SupervisorJob(NetworkManager.SUPERVISOR)
    
    /**
     * Enqueues the [task] to be processed by the appropriate coroutine.
     * Also queries protection in case of [ProtectedNodeNetworkTask].
     */
    fun queueTask(task: NetworkTask): Unit = runBlocking {
        queueLock.withLock {
            if (task is ProtectedNodeNetworkTask)
                protectionResults[task] = CoroutineScope(protectionSupervisor).async(Dispatchers.Default) { queryProtection(task.node) }
            
            // ensure load chunk task is queued before any other task that might need its data
            val chunkPos = task.chunkPos
            if (task is LoadChunkTask) {
                // ignore duplicate chunk load requests
                if (chunkPos in loadedChunks)
                    return@runBlocking
                
                channel.send(task)
                loadedChunks += chunkPos
                taskBacklog.remove(chunkPos)?.forEach { channel.send(it) }
            } else if (task is UnloadChunkTask) {
                // ignore duplicate chunk unload requests
                if (chunkPos !in loadedChunks)
                    return@runBlocking
                
                channel.send(task)
                loadedChunks -= chunkPos
            } else if (chunkPos !in loadedChunks) {
                taskBacklog.getOrPut(chunkPos, ::ArrayList) += task
            } else {
                channel.send(task)
            }
        }
    }
    
    /**
     * Closes the [channel] and waits for all queued tasks to be processed.
     */
    suspend fun awaitShutdown() {
        protectionSupervisor.cancel()
        channel.close()
        job.join()
    }
    
    /**
     * Queries the block use protection in all 6 cartesian directions around [node] asynchronously
     * and returns the [ProtectionResult].
     */
    private suspend fun queryProtection(node: NetworkNode): ProtectionResult = coroutineScope {
        val owner = node.owner
        if (owner != null) {
            CUBE_FACES
                .map { face -> ProtectionManager.canUseBlockAsync(owner, null, node.pos.advance(face, 1)) }
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
        try {
            val data = protoNetwork.immutableCopy()
            val network = protoNetwork.type.createNetwork(data)
            protoNetwork.network = network
            protoNetwork.markClean()
            return network
        } catch(e: Exception) {
            throw Exception("Failed to build dirty network: $protoNetwork", e)
        }
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