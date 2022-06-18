package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.util.data.HashUtils
import java.util.*

internal abstract class NetworkedVanillaTileEntity internal constructor(state: VanillaTileEntityState) : VanillaTileEntity(state), NetworkEndPoint {
    
    override val location = blockState.pos.location
    override val uuid = HashUtils.getUUID(blockState.pos)
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = HashMap()
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = HashMap()
    
    override fun handleInitialized() {
        NetworkManager.queueAsync { it.addEndPoint(this) }
    }
    
    override fun handleRemoved(unload: Boolean) {
        if (!unload) NetworkManager.queueAsync { it.removeEndPoint(this) }
    }
    
    override fun retrieveSerializedNetworks(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveOrNull<HashMap<NetworkType, EnumMap<BlockFace, UUID>>>("networks")
    
    override fun retrieveSerializedConnectedNodes(): Map<NetworkType, Map<BlockFace, UUID>>? =
        retrieveOrNull<HashMap<NetworkType, EnumMap<BlockFace, UUID>>>("connectedNodes")
    
    override fun saveData() {
        storeData("networks", serializeNetworks())
        storeData("connectedNodes", serializeConnectedNodes())
    }
    
}