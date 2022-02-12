package xyz.xenondevs.nova.tileentity.network.fluid

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.fluid.channel.FluidNetworkChannel
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.getOrSet

class FluidNetwork : Network {
    
    override val type = NetworkType.FLUID
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<FluidBridge>()
    private val channels: Array<FluidNetworkChannel?> = arrayOfNulls(CHANNEL_AMOUNT)
    
    private var transferRate = DEFAULT_TRANSFER_RATE
    
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
    
    override fun addBridge(bridge: NetworkBridge) {
        if (bridge in _nodes) return
        require(bridge is FluidBridge) { "Illegal Bridge Type" }
        
        _nodes += bridge
        bridges += bridge
        transferRate = bridge.fluidTransferRate
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        if (endPoint in _nodes) return
        val fluidHolder = endPoint.holders[NetworkType.FLUID]
        require(fluidHolder is FluidHolder) { "Illegal NetworkEndPoint Type" }
        
        val channel = channels.getOrSet(fluidHolder.channels[face]!!) { FluidNetworkChannel() }
        channel.addHolder(fluidHolder, face)
        
        _nodes += endPoint
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is NetworkEndPoint) {
            val fluidHolder = node.holders[NetworkType.FLUID] as FluidHolder
            fluidHolder.channels.values.toSet().asSequence()
                .mapNotNull { channels[it]?.to(it) }
                .onEach { it.first.removeHolder(fluidHolder) }
                .filter { it.first.isEmpty() }
                .forEach { channels[it.second] = null }
        } else if (node is FluidBridge) bridges -= node
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
        private val DEFAULT_TRANSFER_RATE = DEFAULT_CONFIG.getLong("network.fluid.default_transfer_rate")!!
        
    }
    
}

