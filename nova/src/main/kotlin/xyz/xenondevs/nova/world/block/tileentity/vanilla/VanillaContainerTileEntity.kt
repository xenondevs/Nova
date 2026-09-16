package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedShulkerBoxInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaContainerTileEntity(
    blockEntity: BaseContainerBlockEntity
) : ItemStorageVanillaTileEntity(blockEntity) {
    
    override val itemHolder: ItemHolder
    
    init {
        val container = SimpleItemStackContainer(blockEntity.contents)
        val inventory = if (blockEntity is ShulkerBoxBlockEntity)
            NetworkedShulkerBoxInventory(container, blockEntity)
        else NetworkedNMSInventory(container, blockEntity)
        
        itemHolder = StaticVanillaItemHolder(
            blockEntity,
            CubeFaceMap<NetworkedInventory?>(inventory)
        )
    }
    
}
