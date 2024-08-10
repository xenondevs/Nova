package xyz.xenondevs.nova.world.block.tileentity.network

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.event.world.WorldUnloadEvent
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.task.AddBridgeTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.AddEndPointTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.CustomReadTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.CustomUncertainTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.CustomWriteTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.LoadChunkTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.NetworkTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.RemoveBridgeTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.RemoveEndPointTask
import xyz.xenondevs.nova.world.block.tileentity.network.task.UnloadChunkTask
import xyz.xenondevs.nova.world.block.tileentity.network.type.DefaultNetworkTypes
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import java.util.concurrent.ConcurrentHashMap

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [DefaultNetworkTypes::class, ProtectionManager::class]
)
object NetworkManager : Listener {
    
    private val nodeProviders = mutableListOf(NovaNetworkNodeProvider, VanillaNetworkNodeProvider)
    private val configurators = ConcurrentHashMap<World, NetworkConfigurator>()
    private val ticker = NetworkTicker.create()
    
    /**
     * A sequence of all loaded [NetworkClusters][NetworkCluster].
     */
    val clusters: Sequence<NetworkCluster>
        get() = ticker.clusters
    
    /**
     * A sequence of all loaded [Networks][Network].
     */
    val networks: Sequence<Network<*>>
        get() = ticker.networks
    
    internal val SUPERVISOR = SupervisorJob()
    
    @InitFun(runBefore = [WorldDataManager::class])
    private fun initConfigurators() {
        for (world in Bukkit.getWorlds()) {
            makeConfigurator(world)
        }
    }
    
    @InitFun(runAfter = [WorldDataManager::class])
    private fun runConfigurators() {
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                queueLoadChunk(chunk.pos)
            }
        }
        
        runTaskTimer(0, 1, ticker::tick)
        registerEvents()
    }
    
    @DisableFun(dispatcher = Dispatcher.ASYNC)
    private suspend fun disable() {
        for ((_, configurator) in configurators) {
            configurator.awaitShutdown()
        }
        SUPERVISOR.cancel("NetworkManager disabled")
    }
    
    private fun makeConfigurator(world: World) {
        configurators[world] = NetworkConfigurator(world, ticker)
    }
    
    private fun removeConfigurator(world: World) {
        configurators.remove(world)
    }
    
    /**
     * Queues a custom network task in [pos] that will definitely change the network state.
     */
    fun queueWrite(pos: ChunkPos, write: suspend (NetworkState) -> Unit) {
        queueTask(pos.world!!) { CustomWriteTask(it, pos, write) }
    }
    
    /**
     * Queues a custom network task in [pos] that will definitely not change the network state.
     */
    fun queueRead(pos: ChunkPos, read: suspend (NetworkState) -> Unit) {
        queueTask(pos.world!!) { CustomReadTask(it, pos, read) }
    }
    
    /**
     * Queues a custom network task in [pos] that may or may not change the network state.
     * The [task] then returns whether the network state was changed.
     */
    fun queue(pos: ChunkPos, task: suspend (NetworkState) -> Boolean) {
        queueTask(pos.world!!) { CustomUncertainTask(it, pos, task) }
    }
    
    /**
     * Queues a network task to add [endPoint] to the network state.
     *
     * Should only be called after placing, not during chunk load.
     *
     * @throws IllegalArgumentException If [endPoint] also implements [NetworkBridge].
     */
    fun queueAddEndPoint(endPoint: NetworkEndPoint, updateNodes: Boolean = true) =
        queueTask(endPoint) { AddEndPointTask(it, endPoint, updateNodes) }
    
    /**
     * Queues a network task to add [bridge] to the network state, using the
     * specified [supportedNetworkTypes] and [bridgeFaces].
     *
     * Should only be called after placing, not during chunk load.
     *
     * @throws IllegalArgumentException If [bridge] also implements [NetworkEndPoint].
     */
    fun queueAddBridge(bridge: NetworkBridge, supportedNetworkTypes: Set<NetworkType<*>>, bridgeFaces: Set<BlockFace>, updateNodes: Boolean = true) =
        queueTask(bridge) { AddBridgeTask(it, bridge, supportedNetworkTypes, bridgeFaces, updateNodes) }
    
    /**
     * Queues a network task to remove [endPoint] from the network state.
     *
     * Should only be called after breaking, not during chunk unload.
     *
     * @throws IllegalArgumentException If [endPoint] also implements [NetworkBridge].
     */
    fun queueRemoveEndPoint(endPoint: NetworkEndPoint, updateNodes: Boolean = true) =
        queueTask(endPoint) { RemoveEndPointTask(it, endPoint, updateNodes) }
    
    /**
     * Queues a network task to remove [bridge] from the network state.
     *
     * Should only be called after breaking, not during chunk unload.
     *
     * @throws IllegalArgumentException If [bridge] also implements [NetworkEndPoint].
     */
    fun queueRemoveBridge(bridge: NetworkBridge, updateNodes: Boolean = true) =
        queueTask(bridge) { RemoveBridgeTask(it, bridge, updateNodes) }
    
    /**
     * Queues a network task to load the chunk at [pos].
     */
    private fun queueLoadChunk(pos: ChunkPos) =
        queueTask(pos.world!!) { LoadChunkTask(it, pos) }
    
    /**
     * Queues a network task to unload the chunk at [pos].
     */
    private fun queueUnloadChunk(pos: ChunkPos) =
        queueTask(pos.world!!) { UnloadChunkTask(it, pos) }
    
    /**
     * Queues a network task in the world of [node], using the [makeTask] function to create the task.
     *
     * @throws IllegalArgumentException if [node] is both a [NetworkBridge] and a [NetworkEndPoint].
     */
    private fun queueTask(node: NetworkNode, makeTask: (NetworkState) -> NetworkTask) {
        if (node is NetworkBridge && node is NetworkEndPoint)
            throw IllegalArgumentException("Types that inherit from both NetworkBridge and NetworkEndPoint are not allowed")
        
        queueTask(node.pos.world, makeTask)
    }
    
    /**
     * Queues a network task in [world], using the [makeTask] function to create the task.
     */
    private fun queueTask(world: World, makeTask: (NetworkState) -> NetworkTask) {
        val configurator = configurators[world]
            ?: throw IllegalStateException("No NetworkConfigurator for world $world")
        val task = makeTask(configurator.state)
        configurator.queueTask(task)
    }
    
    /**
     * Registers a new [NetworkNodeProvider], which will be used to discover
     * [NetworkNodes][NetworkNode] during chunk load and end point / bridge add tasks.
     */
    fun registerNetworkNodeProvider(provider: NetworkNodeProvider) {
        nodeProviders += provider
    }
    
    /**
     * Gets all [NetworkNodes][NetworkNode] in the chunk at [pos]
     * using the registered [NetworkNodeProviders][NetworkNodeProvider].
     */
    suspend fun getNodes(pos: ChunkPos): List<NetworkNode> {
        return nodeProviders.flatMap { it.getNodes(pos) }
    }
    
    /**
     * Gets the [NetworkNode] at the specified block [pos] using the registered
     * [NetworkNodeProviders][NetworkNodeProvider] or null if there is none.
     */
    suspend fun getNode(pos: BlockPos): NetworkNode? {
        for (nodeProvider in nodeProviders) {
            val node = nodeProvider.getNode(pos)
            if (node != null)
                return node
        }
        
        return null
    }
    
    @EventHandler
    private fun handleWorldLoad(event: WorldLoadEvent) {
        makeConfigurator(event.world)
    }
    
    @EventHandler
    private fun handleWorldUnload(event: WorldUnloadEvent) {
        removeConfigurator(event.world)
    }
    
    @EventHandler(priority = EventPriority.LOW) // WorldDataManager is LOWEST
    private fun handleChunkLoad(event: ChunkLoadEvent) {
        queueLoadChunk(event.chunk.pos)
    }
    
    @EventHandler
    private fun handleChunkUnload(event: ChunkUnloadEvent) {
        queueUnloadChunk(event.chunk.pos)
    }
    
}