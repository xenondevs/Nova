package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.*
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.enumMapOf
import java.util.*

abstract class NetworkedTileEntity(
    ownerUUID: UUID?,
    material: NovaMaterial,
    data: JsonObject,
    armorStand: ArmorStand
) : TileEntity(ownerUUID, material, data, armorStand), NetworkEndPoint {
    
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { enumMapOf() }
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = enumMapOf()
    
    override fun handleInitialized(first: Boolean) {
        NetworkManager.handleEndPointAdd(this)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        NetworkManager.handleEndPointRemove(this, unload)
    }
    
}