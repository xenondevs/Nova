package xyz.xenondevs.nova.tileentity.network.item

import com.google.common.base.Preconditions
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType.*
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

private typealias ItemFilterList = List<ItemFilter>

private fun ItemFilterList.allowsItem(itemStack: ItemStack) = isEmpty() || any { it.allowsItem(itemStack) }

class ItemNetwork : Network {
    
    override val nodes: Set<NetworkNode>
        get() = _nodes
    
    private val _nodes = HashSet<NetworkNode>()
    private val bridges = HashSet<ItemBridge>()
    private val providers = HashSet<Pair<ItemHolder, BlockFace>>()
    private val consumers = HashSet<Pair<ItemHolder, BlockFace>>()
    
    private val transferRate: Int
        get() = bridges.map { it.itemTransferRate }.minOrNull() ?: 1
    
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
        val itemHolder = endPoint.holders[NetworkType.ITEMS] as ItemHolder
        
        val pair = itemHolder to face
        when (val connectionType = itemHolder.itemConfig[face]!!) {
            
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
        if (node is NetworkEndPoint) {
            val itemHolder = node.holders[NetworkType.ITEMS] as ItemHolder
            providers.removeIf { it.first == itemHolder }
            consumers.removeIf { it.first == itemHolder }
        } else if (node is ItemBridge) {
            bridges -= node
        }
    }
    
    override fun isEmpty() = _nodes.isEmpty()
    
    override fun isValid() = bridges.isNotEmpty() || _nodes.size > 1
    
    override fun handleTick() {
        val transferRate = transferRate
        
        val providerInventories = providers.mapNotNullTo(HashSet()) { it.first.inventories[it.second] }
        val consumerInventories = consumers.mapNotNullTo(HashSet()) { it.first.inventories[it.second] }
        
        val providerFilter = getFilterMap(EXTRACT)
        val consumerFilter = getFilterMap(INSERT)
        
        var availableTransfers = transferRate
        
        for (providerInventory in providerInventories) {
            for ((index, itemStack) in providerInventory.items.withIndex()) {
                if (
                    itemStack == null
                    || availableTransfers == 0
                    || !providerFilter[providerInventory]!!.allowsItem(itemStack)
                ) continue
                
                val transferAmount = min(itemStack.amount, availableTransfers)
                var amountLeft = transferAmount
                
                val availableInventories = ConcurrentHashMap.newKeySet<NetworkedInventory>()
                    .also {
                        it += consumerInventories
                        it -= providerInventory
                    }
                
                while (availableInventories.isNotEmpty() && amountLeft != 0) {
                    for (consumerInventory in availableInventories) {
                        if (consumerFilter[consumerInventory]!!.allowsItem(itemStack)) {
                            val leftover = consumerInventory.addItem(itemStack.clone().also { it.amount = amountLeft })
                            if (leftover == null) {
                                amountLeft = 0
                                break
                            } else {
                                availableInventories -= consumerInventory
                                amountLeft = leftover.amount
                            }
                        } else availableInventories -= consumerInventory
                    }
                }
                
                val transferredAmount = transferAmount - amountLeft
                itemStack.amount -= transferredAmount
                availableTransfers -= transferredAmount
                
                if (itemStack != providerInventory.getItem(index)) providerInventory.setItem(index, if (itemStack.amount == 0) null else itemStack)
            }
        }
    }
    
    // TODO: optimize
    private fun getFilterMap(type: ItemConnectionType): Map<NetworkedInventory, ItemFilterList> {
        val itemStorages = when (type) {
            INSERT -> consumers
            EXTRACT -> providers
            else -> throw UnsupportedOperationException()
        }
        
        val filterMap = HashMap<NetworkedInventory, MutableList<ItemFilter>>()
        itemStorages.forEach { (itemHolder, face) ->
            val inventory = itemHolder.inventories[face]!!
            val filterList = filterMap[inventory] ?: ArrayList()
            val node = itemHolder.endPoint.getNearbyNodes()[face]
            if (node is ItemBridge) {
                val filter = node.getFilter(type, face.oppositeFace) ?: ItemFilter(false, emptyArray())
                filterList.add(filter)
            }
            filterMap[inventory] = filterList
        }
        
        return filterMap
    }
    
}