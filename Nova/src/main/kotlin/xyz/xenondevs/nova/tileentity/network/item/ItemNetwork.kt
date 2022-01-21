package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.item.channel.ItemNetworkChannel
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.getOrSet

class ItemNetwork : Network {
    
    override val type = NetworkType.ITEMS
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<ItemBridge>()
    private val channels: Array<ItemNetworkChannel?> = arrayOfNulls(CHANNEL_AMOUNT)
    
    private var transferRate = DEFAULT_TRANSFER_RATE
    
    private var nextChannel = 0
    
    override fun addAll(network: Network) {
        require(network !== this) { "Can't add to self" }
        require(network is ItemNetwork) { "Illegal Network Type" }
        
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
        require(bridge is ItemBridge) { "Illegal Bridge Type" }
        
        _nodes += bridge
        bridges += bridge
        transferRate = bridge.itemTransferRate
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        if (endPoint in _nodes) return
        val itemHolder = endPoint.holders[NetworkType.ITEMS]
        require(itemHolder is ItemHolder) { "Illegal NetworkEndPoint Type" }
        
        val channel = channels.getOrSet(itemHolder.channels[face]!!) { ItemNetworkChannel() }
        channel.addHolder(itemHolder, face)
        
        _nodes += endPoint
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is NetworkEndPoint) {
            val itemHolder = node.holders[NetworkType.ITEMS] as ItemHolder
            itemHolder.channels.values.toSet().asSequence()
                .mapNotNull { channels[it]?.to(it) }
                .onEach { it.first.removeHolder(itemHolder) }
                .filter { it.first.isEmpty() }
                .forEach { channels[it.second] = null }
        } else if (node is ItemBridge) bridges -= node
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun isValid() = bridges.isNotEmpty() || _nodes.size > 1
    
    override fun handleTick() {
        val startingChannel = nextChannel
        var transfersLeft = transferRate
        do {
            transfersLeft = channels[nextChannel]?.distributeItems(transfersLeft) ?: transfersLeft
            
            nextChannel++
            if (nextChannel >= channels.size) nextChannel = 0
        } while (transfersLeft != 0 && nextChannel != startingChannel)
    }
    
    companion object {
        
        const val CHANNEL_AMOUNT = 4
        private val DEFAULT_TRANSFER_RATE = DEFAULT_CONFIG.getInt("network.item.default_transfer_rate")!!
        
    }
    
}