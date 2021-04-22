package xyz.xenondevs.nova.network.item

import com.google.common.base.Preconditions
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.network.Network
import xyz.xenondevs.nova.network.NetworkBridge
import xyz.xenondevs.nova.network.NetworkEndPoint
import xyz.xenondevs.nova.network.NetworkNode
import xyz.xenondevs.nova.network.item.ItemConnectionType.*
import xyz.xenondevs.nova.network.item.inventory.NetworkedInventory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

class ItemNetwork : Network {
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<ItemBridge>()
    private val providers = HashSet<Pair<ItemStorage, BlockFace>>()
    private val consumers = HashSet<Pair<ItemStorage, BlockFace>>()
    
    private val transferRate: Int
        get() = bridges.map { it.itemTransferRate }.minOrNull() ?: 0
    
    override fun addAll(network: Network) {
        Preconditions.checkArgument(network is ItemNetwork, "Illegal Network Type")
        network as ItemNetwork
        
        _nodes += network._nodes
        providers += network.providers
        consumers += network.consumers
        bridges += network.bridges
    }
    
    override fun addBridge(bridge: NetworkBridge) {
        Preconditions.checkArgument(bridge is ItemBridge, "Illegal Bridge Type")
        _nodes += bridge
        bridges += bridge as ItemBridge
    }
    
    override fun addEndPoint(endPoint: NetworkEndPoint, face: BlockFace) {
        Preconditions.checkArgument(endPoint is ItemStorage, "Illegal EndPoint Type")
        endPoint as ItemStorage
        
        val pair = endPoint to face
        when (val connectionType = endPoint.itemConfig[face]!!) {
            
            EXTRACT -> providers += pair
            INSERT -> consumers += pair
            BUFFER -> {
                providers += pair
                consumers += pair
            }
            
            else -> throw IllegalArgumentException("Illegal ConnectionType: $connectionType")
            
        }
        
        _nodes += endPoint
    }
    
    override fun removeNode(node: NetworkNode) {
        _nodes -= node
        if (node is ItemStorage) {
            providers.removeIf { it.first == node }
            consumers.removeIf { it.first == node }
        } else if (node is ItemBridge) {
            bridges -= node
        }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun handleTick() {
        val transferRate = transferRate
        val providerInventories = providers.mapNotNullTo(HashSet()) { it.first.inventories[it.second] }
        val consumerInventories = consumers.mapNotNullTo(HashSet()) { it.first.inventories[it.second] }
        
        var availableTransfers = transferRate
        
        for (providerInventory in providerInventories) {
            for ((index, itemStack) in providerInventory.items.withIndex()) {
                if (itemStack == null || availableTransfers == 0) continue
                
                val transferAmount = min(itemStack.amount, availableTransfers)
                var amountLeft = transferAmount
                
                val emptyInventories = ConcurrentHashMap.newKeySet<NetworkedInventory>()
                    .also {
                        it += consumerInventories
                        it -= providerInventory
                    }
                
                while (emptyInventories.isNotEmpty() && amountLeft != 0) {
                    for (consumerInventory in emptyInventories) {
                        val leftover = consumerInventory.addItem(itemStack.clone().also { it.amount = amountLeft })
                        if (leftover == null) {
                            amountLeft = 0
                            break
                        } else {
                            emptyInventories -= consumerInventory
                            amountLeft = leftover.amount
                        }
                    }
                }
                
                val transferredAmount = transferAmount - amountLeft
                itemStack.amount -= transferredAmount
                availableTransfers -= transferredAmount
                
                providerInventory.setItem(index, if (itemStack.amount == 0) null else itemStack)
            }
        }
    }
    
}