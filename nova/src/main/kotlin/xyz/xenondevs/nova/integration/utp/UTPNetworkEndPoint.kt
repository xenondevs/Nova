package xyz.xenondevs.nova.integration.utp

import org.bukkit.Location
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.serialization.DataHolder
import xyz.xenondevs.nova.data.world.block.state.UTPBlockState
import xyz.xenondevs.nova.integration.utp.energy.UTPEnergyHolderWrapper
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.NetworkType
import java.util.*

internal class UTPNetworkEndPoint(
    val blockState: UTPBlockState
) : DataHolder(false), NetworkEndPoint {
    
    override val location: Location = blockState.pos.location
    override val uuid: UUID = blockState.uuid
    override val data = blockState.data
    
    override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = HashMap()
    override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = HashMap()
    
    override val holders: Map<NetworkType, EndPointDataHolder> by lazy {
        val holders = HashMap<NetworkType, EndPointDataHolder>()
        
        val energyStorage = blockState.energyStorage.get()
        if (energyStorage != null)
            holders[NetworkType.ENERGY] = UTPEnergyHolderWrapper.of(this, energyStorage)
        
        return@lazy holders
    }
    
    fun handleInitialized(placed: Boolean) {
        if (placed) NetworkManager.queueAsync { it.addEndPoint(this, true) }
    }
    
    fun handleRemoved(broken: Boolean) {
        if (broken) NetworkManager.queueAsync { it.removeEndPoint(this, true) }
    }
    
    fun saveData() {
        storeData("networks", serializeNetworks())
        storeData("connectedNodes", serializeConnectedNodes())
    }
    
}