package xyz.xenondevs.nova.tileentity.network.fluid.channel

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.pollFirstWhere
import xyz.xenondevs.commons.collections.selectValues
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import java.util.*

internal sealed interface FluidConfiguration {
    
    val fluidHolder: FluidHolder
    val faces: Set<BlockFace>
    val container: NetworkedFluidContainer
    val type: NetworkConnectionType
    
}

private class DefaultFluidConfiguration(
    override val fluidHolder: FluidHolder,
    override val faces: Set<BlockFace>,
    override val container: NetworkedFluidContainer,
    override val type: NetworkConnectionType
) : FluidConfiguration {
    
    val priority: Int = when (type) {
        NetworkConnectionType.INSERT -> fluidHolder.insertPriorities
        NetworkConnectionType.EXTRACT -> fluidHolder.extractPriorities
        else -> throw IllegalArgumentException()
    }.selectValues(faces).maxOrNull()!!
    
    fun component1() = container
    fun component2() = priority
    
}

/**
 * The [FluidBufferConfiguration] is special configuration for [Fluid Holders][FluidHolder] that are
 * connected using [NetworkConnectionType.BUFFER].
 *
 * If the insert and extract priority are unequal, the [NetworkedFluidContainer] of this configuration is treated as a
 * consumer or provider until a priority level is reached which includes both insert and extract, then the
 * [NetworkedFluidContainer] will be considered a fluid buffer.
 */
private class FluidBufferConfiguration(
    override val fluidHolder: FluidHolder,
    override val faces: Set<BlockFace>,
    override val container: NetworkedFluidContainer,
) : FluidConfiguration {
    
    override val type = NetworkConnectionType.BUFFER
    
    /**
     * The [NetworkConnectionType] this [FluidBufferConfiguration] has when [bufferPriority] has not been reached yet.
     */
    val nonBufferType: NetworkConnectionType?
    
    /**
     * The base priority level for this [FluidBufferConfiguration] to be used.
     */
    val defaultPriority: Int
    
    /**
     * The priority level at which the [nonBufferType] is ignored and this [FluidBufferConfiguration] is considered
     * a fluid buffer.
     */
    val bufferPriority: Int
    
    init {
        val insertPriority = fluidHolder.insertPriorities.selectValues(faces).maxOrNull()!!
        val extractPriority = fluidHolder.extractPriorities.selectValues(faces).maxOrNull()!!
        
        if (insertPriority != extractPriority) {
            if (insertPriority > extractPriority) {
                nonBufferType = NetworkConnectionType.INSERT
                defaultPriority = insertPriority
                bufferPriority = extractPriority
            } else {
                nonBufferType = NetworkConnectionType.EXTRACT
                defaultPriority = extractPriority
                bufferPriority = insertPriority
            }
        } else {
            nonBufferType = null
            defaultPriority = insertPriority
            bufferPriority = insertPriority
        }
    }
    
    fun component1() = container
    fun component2() = nonBufferType
    fun component3() = defaultPriority
    fun component4() = bufferPriority
    
}

private fun createFluidConfiguration(fluidHolder: FluidHolder, face: BlockFace): FluidConfiguration =
    createFluidConfiguration(fluidHolder, setOf(face), fluidHolder.containerConfig[face]!!, fluidHolder.connectionConfig[face]!!)

private fun createFluidConfiguration(
    fluidHolder: FluidHolder,
    faces: Set<BlockFace>,
    container: NetworkedFluidContainer,
    type: NetworkConnectionType
): FluidConfiguration {
    return when (type) {
        NetworkConnectionType.INSERT, NetworkConnectionType.EXTRACT -> DefaultFluidConfiguration(fluidHolder, faces, container, type)
        NetworkConnectionType.BUFFER -> FluidBufferConfiguration(fluidHolder, faces, container)
        else -> throw IllegalArgumentException()
    }
}

private fun mergeFluidConfigurations(
    first: FluidConfiguration,
    second: FluidConfiguration
): FluidConfiguration {
    val newType = NetworkConnectionType.of(first.type.insert || second.type.insert, first.type.extract || second.type.extract)
    val faces = first.faces + second.faces
    
    return createFluidConfiguration(first.fluidHolder, faces, first.container, newType)
}

private fun Iterable<FluidConfiguration>.mapToContainer(): List<NetworkedFluidContainer> = map { it.container }

internal class FluidNetworkChannel {
    
    private val holders = HashMap<FluidHolder, HashSet<FluidConfiguration>>()
    
    private val consumerConfigurations = HashSet<FluidConfiguration>()
    private val providerConfigurations = HashSet<FluidConfiguration>()
    private val bufferConfigurations = HashSet<FluidConfiguration>()
    
    private var fluidDistributor: FluidDistributor? = null
    
    private fun getConfigurations(fluidHolder: FluidHolder) = holders.getOrPut(fluidHolder) { HashSet() }
    
    fun addAll(otherChannel: FluidNetworkChannel) {
        require(this !== otherChannel) { "Can't add to self" }
        
        holders += otherChannel.holders
        consumerConfigurations += otherChannel.consumerConfigurations
        providerConfigurations += otherChannel.providerConfigurations
        bufferConfigurations += otherChannel.bufferConfigurations
        
        createDistributor()
    }
    
    fun addHolder(holder: FluidHolder, face: BlockFace, createDistributor: Boolean) {
        val config = createOrMergeConfiguration(holder, face)
        
        val configSet = when (config.type) {
            NetworkConnectionType.INSERT -> consumerConfigurations
            NetworkConnectionType.EXTRACT -> providerConfigurations
            NetworkConnectionType.BUFFER -> bufferConfigurations
            else -> throw UnsupportedOperationException()
        }
        
        getConfigurations(holder) += config
        configSet += config
        
        if (createDistributor) createDistributor()
    }
    
    fun removeHolder(holder: FluidHolder, createDistributor: Boolean) {
        holders[holder]?.forEach {
            consumerConfigurations -= it
            providerConfigurations -= it
            bufferConfigurations -= it
        }
        
        holders -= holder
        
        if (createDistributor) createDistributor()
    }
    
    fun isEmpty() = holders.isEmpty()
    
    private fun createOrMergeConfiguration(holder: FluidHolder, face: BlockFace): FluidConfiguration {
        val config = createFluidConfiguration(holder, face)
        
        val configs = holders[holder]
        if (configs != null) {
            val oldConfig = configs.pollFirstWhere { it.container == config.container }
            if (oldConfig != null) {
                consumerConfigurations -= oldConfig
                providerConfigurations -= oldConfig
                bufferConfigurations -= oldConfig
                
                return mergeFluidConfigurations(oldConfig, config)
            }
        }
        
        return config
    }
    
    fun createDistributor() {
        val providers = providerConfigurations.isNotEmpty()
        val consumers = consumerConfigurations.isNotEmpty()
        val buffers = bufferConfigurations.isNotEmpty()
        
        fluidDistributor = if ((providers && (consumers || buffers)) || (consumers && (providers || buffers))) {
            FluidDistributor(computeAvailableContainers())
        } else null
    }
    
    private fun convertConfigurations(configurations: Set<FluidConfiguration>): Map<Int, List<FluidConfiguration>> {
        val map = HashMap<Int, ArrayList<FluidConfiguration>>()
        configurations.forEach {
            if (it is DefaultFluidConfiguration) {
                map.getOrPut(it.priority) { ArrayList() } += it
            } else if (it is FluidBufferConfiguration) {
                map.getOrPut(it.bufferPriority) { ArrayList() } += it
                if (it.nonBufferType != null)
                    map.getOrPut(it.defaultPriority) { ArrayList() } += it
            }
        }
        
        return map
    }
    
    // TODO: optimize
    private fun computeAvailableContainers(): List<Triple<List<NetworkedFluidContainer>, List<NetworkedFluidContainer>, List<NetworkedFluidContainer>>> {
        val prioritizedFluidContainers = ArrayList<Triple<List<NetworkedFluidContainer>, List<NetworkedFluidContainer>, List<NetworkedFluidContainer>>>()
        
        val consumers = convertConfigurations(consumerConfigurations)
        val providers = convertConfigurations(providerConfigurations)
        val buffers = convertConfigurations(bufferConfigurations)
        
        var lastConfigs: Triple<Set<FluidConfiguration>, Set<FluidConfiguration>, Set<FluidConfiguration>>? = null
        
        TreeSet<Int>(Comparator.reverseOrder())
            .apply {
                addAll(consumers.keys)
                addAll(providers.keys)
                addAll(buffers.keys)
            }.forEach { priority ->
                val consumerContainersForPriority = HashSet<FluidConfiguration>()
                val providerContainersForPriority = HashSet<FluidConfiguration>()
                val bufferContainersForPriority = HashSet<FluidConfiguration>()
                
                if (lastConfigs != null) {
                    consumerContainersForPriority += lastConfigs!!.first
                    providerContainersForPriority += lastConfigs!!.second
                    bufferContainersForPriority += lastConfigs!!.third
                }
                
                consumers[priority]?.forEach(consumerContainersForPriority::add)
                providers[priority]?.forEach(providerContainersForPriority::add)
                buffers[priority]?.forEach { cfg ->
                    cfg as FluidBufferConfiguration
                    
                    if (priority == cfg.bufferPriority) {
                        bufferContainersForPriority += cfg
                        
                        if (cfg.nonBufferType == NetworkConnectionType.INSERT) {
                            consumerContainersForPriority -= cfg
                        } else if (cfg.nonBufferType == NetworkConnectionType.EXTRACT) {
                            providerContainersForPriority -= cfg
                        }
                    } else if (cfg.nonBufferType == NetworkConnectionType.INSERT) {
                        consumerContainersForPriority += cfg
                    } else if (cfg.nonBufferType == NetworkConnectionType.EXTRACT) {
                        providerContainersForPriority += cfg
                    }
                }
                
                lastConfigs = Triple(consumerContainersForPriority, providerContainersForPriority, bufferContainersForPriority)
                prioritizedFluidContainers += Triple(
                    consumerContainersForPriority.mapToContainer(),
                    providerContainersForPriority.mapToContainer(),
                    bufferContainersForPriority.mapToContainer()
                )
            }
        
        return prioritizedFluidContainers
    }
    
    fun distributeFluids(transferAmount: Long): Long {
        return fluidDistributor?.distribute(transferAmount) ?: transferAmount
    }
    
}
