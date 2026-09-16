package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.entity.BlockEntity
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.craftbukkit.block.CraftBlock
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import java.util.*

internal abstract class NetworkedVanillaTileEntity(
    blockEntity: BlockEntity
) : VanillaTileEntity(blockEntity), NetworkEndPoint {
    
    final override val block: Block = CraftBlock.at(requireNotNull(blockEntity.level), blockEntity.blockPos)
    final override val owner: OfflinePlayer? = resolveOwner(blockEntity)
    override val linkedNodes: Set<NetworkNode> = emptySet()
    
    override val isValid: Boolean
        get() = !blockEntity.isRemoved
    
    private companion object {
        
        val OWNER_UUID = Key.key("nova", "owner_uuid")
        
        fun resolveOwner(blockEntity: BlockEntity): OfflinePlayer? {
            val pdc = blockEntity.persistentDataContainer
            val placementOwnerUuid = VanillaTileEntityManager.getPlacementOwner(blockEntity.blockPos)
            if (placementOwnerUuid != null) {
                pdc[OWNER_UUID] = placementOwnerUuid
                blockEntity.setChanged()
                return Bukkit.getOfflinePlayer(placementOwnerUuid)
            }
            
            val ownerUuid: UUID? = pdc[OWNER_UUID]
            return ownerUuid?.let(Bukkit::getOfflinePlayer)
        }
        
    }
    
}
