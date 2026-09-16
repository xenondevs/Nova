package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaBrewingStandTileEntity(
    blockEntity: BrewingStandBlockEntity
) : ItemStorageVanillaTileEntity(blockEntity) {
    
    override val itemHolder: ItemHolder
    
    init {
        val bottles = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(0, 3)), blockEntity)
        val ingredient = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(3, 4)), blockEntity)
        val fuel = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(4, 5)), blockEntity)
        val inventories = CubeFaceMap<NetworkedInventory?>(
            north = fuel,
            east = fuel,
            south = fuel,
            west = fuel,
            up = ingredient,
            down = bottles
        )
        
        itemHolder = StaticVanillaItemHolder(
            blockEntity,
            inventories
        )
    }
    
}
