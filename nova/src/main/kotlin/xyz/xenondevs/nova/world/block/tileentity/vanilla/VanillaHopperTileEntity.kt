package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.entity.HopperBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaHopperTileEntity(
    blockEntity: HopperBlockEntity
) : ItemStorageVanillaTileEntity(blockEntity) {
    
    override val itemHolder: ItemHolder
    
    init {
        val facing = blockEntity.blockState.getValue(HopperBlock.FACING).blockFace
        val inventory = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents), blockEntity)
        val connectionConfig = CubeFaceMap(NetworkConnectionType.NONE)
            .with(BlockFace.UP, NetworkConnectionType.INSERT)
            .with(facing, NetworkConnectionType.EXTRACT)
        
        itemHolder = StaticVanillaItemHolder(
            blockEntity,
            CubeFaceMap<NetworkedInventory?>(inventory),
            connectionConfig
        )
    }
    
}
