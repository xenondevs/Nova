package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

abstract class NetworkedVanillaTileEntity internal constructor(state: VanillaTileEntityState) : VanillaTileEntity(state), NetworkEndPoint {
    
    override val location = blockState.pos.location
    override val uuid = HashUtils.getUUID(blockState.pos)
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = emptyEnumMap()
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = emptyEnumMap()
    
    override fun handleInitialized() {
        NetworkManager.queueAsync { it.addEndPoint(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (!unload) NetworkManager.queueAsync { it.removeEndPoint(this) }
    }
    
    override fun retrieveSerializedNetworks(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveEnumMapOrNull("networks")
    
    override fun retrieveSerializedConnectedNodes(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveEnumMapOrNull("connectedNodes")
    
    override fun saveData() {
        storeEnumMap("networks", serializeNetworks())
        storeEnumMap("connectedNodes", serializeConnectedNodes())
    }
    
}