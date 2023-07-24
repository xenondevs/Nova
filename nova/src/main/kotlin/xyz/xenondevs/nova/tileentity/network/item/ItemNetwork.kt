package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.item.channel.ItemNetworkChannel
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import java.util.*

class ItemNetwork(override val uuid: UUID, private val local: Boolean) : Network {
    
    override val type = DefaultNetworkTypes.ITEMS
    
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
    
    override fun addAll(nodes: Iterable<Pair<BlockFace?, NetworkNode>>) {
        nodes.mapNotNull { (face, node) ->
            if (node is NetworkBridge) {
                addBridge(node)
            } else if (node is NetworkEndPoint && face != null) {
                return@mapNotNull addEndPoint(node, face, false)
            }
            return@mapNotNull null
        }.forEach { it.createDistributor() }
    }
    
    override fun addBridge(bridge: NetworkBridge) {
        if (bridge in _nodes) return
        require(bridge is ItemBridge) { "Illegal Bridge Type" }
        
        _nodes += bridge
        bridges += bridge
        transferRate = bridge.itemTransferRate
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        addEndPoint(endPoint, face, true)
    }
    
    private fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace, createDistributor: Boolean): ItemNetworkChannel {
        val itemHolder = endPoint.holders[DefaultNetworkTypes.ITEMS]
        require(itemHolder is ItemHolder) { "Illegal NetworkEndPoint Type" }
        
        val connectionType = if (local && endPoint is VanillaTileEntity)
            NetworkConnectionType.BUFFER
        else itemHolder.connectionConfig[face]!!
        
        val channel = channels.getOrSet(itemHolder.channels[face]!!) { ItemNetworkChannel() }
        channel.addHolder(itemHolder, face, connectionType, createDistributor)
        
        _nodes += endPoint
        
        return channel
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is NetworkEndPoint) {
            val itemHolder = node.holders[DefaultNetworkTypes.ITEMS]
            require(itemHolder is ItemHolder) { "Illegal NetworkEndPoint Type" }
            
            itemHolder.channels.values.toSet().asSequence()
                .mapNotNull { channels[it]?.to(it) }
                .onEach { it.first.removeHolder(itemHolder, true) }
                .filter { it.first.isEmpty() }
                .forEach { channels[it.second] = null }
        } else if (node is ItemBridge) bridges -= node
    }
    
    override fun removeAll(nodes: List<NetworkNode>) {
        val channelsToUpdate = ArrayList<ItemNetworkChannel>()
        
        nodes.forEach { node ->
            _nodes -= node
            if (node is NetworkEndPoint) {
                val itemHolder = node.holders[DefaultNetworkTypes.ITEMS]
                require(itemHolder is ItemHolder) { "Illegal NetworkEndPoint Type" }
                
                itemHolder.channels.values.toSet().asSequence()
                    .mapNotNull { channels[it]?.to(it) }
                    .onEach {
                        val channel = it.first
                        channel.removeHolder(itemHolder, false)
                        channelsToUpdate += channel
                    }
                    .filter { it.first.isEmpty() }
                    .forEach { channels[it.second] = null }
            } else if (node is ItemBridge) bridges -= node
        }
        
        channelsToUpdate.forEach { it.createDistributor() }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun isValid() = bridges.isNotEmpty() || _nodes.size > 1
    
    override fun reload() {
        bridges.firstOrNull()?.itemTransferRate ?: DEFAULT_TRANSFER_RATE
    }
    
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
        private val DEFAULT_TRANSFER_RATE by MAIN_CONFIG.entry<Int>("network", "item", "default_transfer_rate")
        
    }
    
}