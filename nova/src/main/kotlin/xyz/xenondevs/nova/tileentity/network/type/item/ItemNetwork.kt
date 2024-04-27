package xyz.xenondevs.nova.tileentity.network.type.item

import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkData
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.item.channel.ItemNetworkChannel
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import kotlin.math.min
import kotlin.math.roundToInt

class ItemNetwork(networkData: NetworkData) : Network, NetworkData by networkData {
    
    private val channels: Array<ItemNetworkChannel?> = arrayOfNulls(CHANNEL_AMOUNT)
    private val transferRate: Int
    
    private var nextChannel = 0
    
    init {
        var transferRate = DEFAULT_TRANSFER_RATE
        
        for ((node, faces) in nodes.values) {
            if (node is NetworkEndPoint) {
                val itemHolder = node.holders.firstInstanceOfOrNull<ItemHolder>()
                    ?: continue
                
                for ((face, channelId) in itemHolder.channels) {
                    if (face in faces) {
                        val channel = channels.getOrSet(channelId, ::ItemNetworkChannel)
                        channel.addHolder(itemHolder, face, itemHolder.connectionConfig[face]!!)
                    }
                }
            } else if (node is ItemBridge) {
                transferRate = min(transferRate, node.itemTransferRate)
            }
        }
        
        this.transferRate = transferRate
        
        for (channel in channels)
            channel?.createDistributor()
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
        
        private val ITEM_NETWORK = MAIN_CONFIG.node("network", "item")
        val TICK_DELAY_PROVIDER: Provider<Int> = ITEM_NETWORK.entry<Int>("tick_delay")
        val DEFAULT_TRANSFER_RATE: Int by combinedProvider(ITEM_NETWORK.entry<Double>("default_transfer_rate"), TICK_DELAY_PROVIDER)
            .map { (defaultTransferRate, tickDelay) -> (defaultTransferRate * tickDelay).roundToInt() }
            .map { defaultTransferRate -> if (defaultTransferRate < 0) Int.MAX_VALUE else defaultTransferRate }
        val CHANNEL_AMOUNT: Int by ITEM_NETWORK.entry<Int>("channel_amount")
        
    }
    
}