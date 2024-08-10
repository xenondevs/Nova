package xyz.xenondevs.nova.world.block.tileentity.network.type.item

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.world.block.tileentity.network.Network
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkData
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.channel.ItemChannelsBuilder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.channel.ItemDistributor
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import kotlin.math.min
import kotlin.math.roundToInt

// TODO: block updates
class ItemNetwork internal constructor(
    networkData: NetworkData<ItemNetwork>
) : Network<ItemNetwork>, NetworkData<ItemNetwork> by networkData {
    
    internal val channels: Array<ItemDistributor?>
    private val transferRate: Int
    val complexity: Int
    
    private var nextChannel = 0
    
    init {
        var transferRate = DEFAULT_TRANSFER_RATE
        var complexity = 0
        val channelsBuilder = ItemChannelsBuilder()
        for ((node, faces) in nodes.values) {
            if (node is NetworkEndPoint) {
                val itemHolder = node.holders.firstInstanceOfOrNull<ItemHolder>()
                    ?: continue
                
                channelsBuilder.addHolder(itemHolder, faces)
                complexity++
            } else if (node is ItemBridge) {
                transferRate = min(transferRate, node.itemTransferRate)
            }
        }
        
        this.transferRate = transferRate
        this.complexity = complexity
        channels = channelsBuilder.build()
    }
    
    internal fun tick() {
        if (MAX_COMPLEXITY != -1 && complexity > MAX_COMPLEXITY)
            return
        
        val startingChannel = nextChannel
        var transfersLeft = transferRate
        do {
            val distributor = channels[nextChannel]
            if (distributor != null)
                transfersLeft = distributor.distribute(transfersLeft)
            
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
        val MAX_COMPLEXITY: Int by ITEM_NETWORK.entry<Int>("max_complexity")
        
        fun validateLocal(from: NetworkEndPoint, to: NetworkEndPoint, face: BlockFace): Boolean {
            val itemHolderFrom = from.holders.firstInstanceOfOrNull<ItemHolder>() ?: return false
            val itemHolderTo = to.holders.firstInstanceOfOrNull<ItemHolder>() ?: return false
            val conFrom = itemHolderFrom.connectionConfig[face]
            val conTo = itemHolderTo.connectionConfig[face.oppositeFace]
            
            return conFrom != conTo || conFrom == NetworkConnectionType.BUFFER
        }
        
    }
    
}