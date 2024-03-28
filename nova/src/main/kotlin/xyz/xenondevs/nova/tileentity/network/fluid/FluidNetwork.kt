package xyz.xenondevs.nova.tileentity.network.fluid

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.fluid.channel.FluidNetworkChannel
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import java.util.*

class FluidNetwork(override val uuid: UUID) : Network {
    
    override val type = DefaultNetworkTypes.FLUID
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<FluidBridge>()
    private val channels: Array<FluidNetworkChannel?> = arrayOfNulls(CHANNEL_AMOUNT)
    
    private val transferRate: Long
        get() = bridges.firstOrNull()?.fluidTransferRate ?: DEFAULT_TRANSFER_RATE
    
    private var nextChannel = 0
    
    override fun addAll(network: Network) {
        require(network !== this) { "Can't add to self" }
        require(network is FluidNetwork) { "Illegal Network Type" }
        
        _nodes += network._nodes
        bridges += network.bridges
        
        network.channels.withIndex().forEach { (id, channel) ->
            if (channel == null) return@forEach
            if (channels[id] == null) channels[id] = channel
            else channels[id]!!.addAll(channel)
        }
    }
    
    override fun addAll(nodes: Iterable<Pair<BlockFace?, NetworkNode>>) {
        nodes.asSequence().mapNotNull { (face, node) ->
            if (node is NetworkBridge)
                addBridge(node)
            else if (node is NetworkEndPoint && face != null)
                return@mapNotNull addEndPoint(node, face, false)
            
            return@mapNotNull null
        }.forEach { it.createDistributor() }
    }
    
    override fun addBridge(bridge: NetworkBridge) {
        if (bridge in _nodes) return
        require(bridge is FluidBridge) { "Illegal Bridge Type" }
        
        _nodes += bridge
        bridges += bridge
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        addEndPoint(endPoint, face, true)
    }
    
    private fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace, createDistributor: Boolean): FluidNetworkChannel? {
        if (endPoint in _nodes) return null
        
        val fluidHolder = endPoint.holders[DefaultNetworkTypes.FLUID]
        require(fluidHolder is FluidHolder) { "Illegal NetworkEndPoint Type" }
        
        val channel = channels.getOrSet(fluidHolder.channels[face]!!) { FluidNetworkChannel() }
        channel.addHolder(fluidHolder, face, createDistributor)
        
        _nodes += endPoint
        
        return channel
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is NetworkEndPoint) {
            val fluidHolder = node.holders[DefaultNetworkTypes.FLUID]
            require(fluidHolder is FluidHolder) { "Illegal NetworkEndPoint Type" }
            
            fluidHolder.channels.values.toSet().asSequence()
                .mapNotNull { channels[it]?.to(it) }
                .onEach { it.first.removeHolder(fluidHolder, true) }
                .filter { it.first.isEmpty() }
                .forEach { channels[it.second] = null }
        } else if (node is FluidBridge) bridges -= node
    }
    
    override fun removeAll(nodes: List<NetworkNode>) {
        val nodeChannels = ArrayList<FluidNetworkChannel>()
        
        nodes.forEach { node ->
            _nodes -= node
            if (node is NetworkEndPoint) {
                val fluidHolder = node.holders[DefaultNetworkTypes.FLUID]
                require(fluidHolder is FluidHolder) { "Illegal NetworkEndPoint Type" }
    
                fluidHolder.channels.values.toSet().asSequence()
                    .mapNotNull { channels[it]?.to(it) }
                    .onEach {
                        it.first.removeHolder(fluidHolder, false)
                        nodeChannels += it.first
                    }
                    .filter { it.first.isEmpty() }
                    .forEach { channels[it.second] = null }
            } else if (node is FluidBridge) bridges -= node
        }
        
        nodeChannels.forEach { it.createDistributor() }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun isValid() = bridges.isNotEmpty() || _nodes.size > 1
    
    override fun handleTick() {
        val startingChannel = nextChannel
        var amountLeft = transferRate
        do {
            amountLeft = channels[nextChannel]?.distributeFluids(amountLeft) ?: amountLeft
            
            nextChannel++
            if (nextChannel >= channels.size) nextChannel = 0
        } while (amountLeft != 0L && nextChannel != startingChannel)
    }
    
    companion object {
        
        const val CHANNEL_AMOUNT = 4
        private val DEFAULT_TRANSFER_RATE by MAIN_CONFIG.entry<Long>("network", "fluid", "default_transfer_rate")
        
    }
    
}

