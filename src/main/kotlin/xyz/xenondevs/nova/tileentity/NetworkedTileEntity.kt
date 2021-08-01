package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.armorstand.FakeArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.*
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.enumMapOf
import java.util.*

abstract class NetworkedTileEntity(
    uuid: UUID,
    data: JsonObject,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : TileEntity(uuid, data, material, ownerUUID, armorStand), NetworkEndPoint {
    
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