package xyz.xenondevs.nova.tileentity.network.type.fluid

import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.combinedProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.nova.data.config.MAIN_CONFIG
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkData
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.fluid.channel.FluidNetworkChannel
import xyz.xenondevs.nova.tileentity.network.type.fluid.holder.FluidHolder
import kotlin.math.min
import kotlin.math.roundToLong

// TODO: fix illegal world state access
class FluidNetwork internal constructor(
    networkData: NetworkData<FluidNetwork>
) : Network<FluidNetwork>, NetworkData<FluidNetwork> by networkData {
    
    private val channels: Array<FluidNetworkChannel?> = arrayOfNulls(CHANNEL_AMOUNT)
    private val transferRate: Long
    private val complexity: Int
    
    private var nextChannel = 0
    
    init {
        var transferRate = DEFAULT_TRANSFER_RATE
        var complexity = 0
        
        for ((node, faces) in networkData.nodes.values) {
            if (node is NetworkEndPoint) {
                val fluidHolder = node.holders.firstInstanceOfOrNull<FluidHolder>()
                    ?: continue
                
                for ((face, channelId) in fluidHolder.channels) {
                    if (face in faces) {
                        val channel = channels.getOrSet(channelId, ::FluidNetworkChannel)
                        channel.addHolder(fluidHolder, face)
                    }
                }
                
                complexity++
            } else if (node is FluidBridge) {
                transferRate = min(transferRate, node.fluidTransferRate)
            }
        }
        
        this.transferRate = transferRate
        this.complexity = complexity
        
        for (channel in channels)
            channel?.createDistributor()
    }
    
    fun tick() {
        if (MAX_COMPLEXITY != -1 && complexity > MAX_COMPLEXITY)
            return
        
        val startingChannel = nextChannel
        var amountLeft = transferRate
        do {
            amountLeft = channels[nextChannel]?.distributeFluids(amountLeft) ?: amountLeft
            
            nextChannel++
            if (nextChannel >= channels.size) nextChannel = 0
        } while (amountLeft != 0L && nextChannel != startingChannel)
    }
    
    companion object {
        
        private val FLUID_NETWORK = MAIN_CONFIG.node("network", "fluid")
        val TICK_DELAY_PROVIDER: Provider<Int> = FLUID_NETWORK.entry<Int>("tick_delay")
        val DEFAULT_TRANSFER_RATE: Long by combinedProvider(FLUID_NETWORK.entry<Double>("default_transfer_rate"), TICK_DELAY_PROVIDER)
            .map { (defaultTransferRate, tickDelay) -> (defaultTransferRate * tickDelay).roundToLong() }
            .map { defaultTransferRate -> if (defaultTransferRate < 0) Long.MAX_VALUE else defaultTransferRate }
        val CHANNEL_AMOUNT: Int by FLUID_NETWORK.entry<Int>("channel_amount")
        val MAX_COMPLEXITY: Int by FLUID_NETWORK.entry<Int>("max_complexity")
        
        internal fun validateLocal(from: NetworkEndPoint, to: NetworkEndPoint, face: BlockFace): Boolean {
            val itemHolderFrom = from.holders.firstInstanceOfOrNull<FluidHolder>() ?: return false
            val itemHolderTo = to.holders.firstInstanceOfOrNull<FluidHolder>() ?: return false
            val conFrom = itemHolderFrom.connectionConfig[face]
            val conTo = itemHolderTo.connectionConfig[face.oppositeFace]
            
            return conFrom != conTo || conFrom == NetworkConnectionType.BUFFER
        }
        
    }
    
}

