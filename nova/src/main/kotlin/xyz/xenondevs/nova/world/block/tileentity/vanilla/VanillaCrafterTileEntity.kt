package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.CrafterBlockEntity
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedCrafterInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaCrafterTileEntity(
    blockEntity: CrafterBlockEntity
) : ItemStorageVanillaTileEntity(blockEntity) {
    
    override val itemHolder: ItemHolder
    
    init {
        val inventory = NetworkedCrafterInventory(
            blockEntity,
            SimpleItemStackContainer(blockEntity.contents)
        )
        itemHolder = StaticVanillaItemHolder(
            blockEntity,
            CubeFaceMap<NetworkedInventory?>(inventory)
        )
    }
    
}
