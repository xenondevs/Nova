package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaFurnaceTileEntity(
    blockEntity: AbstractFurnaceBlockEntity
) : ItemStorageVanillaTileEntity(blockEntity) {
    
    override val itemHolder: ItemHolder
    
    init {
        val contents = blockEntity.contents
        val input = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(0, 1)), blockEntity)
        val fuel = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(1, 2)), blockEntity)
        val output = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(2, 3)), blockEntity)
        val inventories = CubeFaceMap<NetworkedInventory?>(fuel)
            .with(BlockFace.UP, input)
            .with(BlockFace.DOWN, output)
        
        itemHolder = StaticVanillaItemHolder(
            blockEntity,
            inventories
        )
    }
    
}
