package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaBrewingStandTileEntity(
    type: Type,
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        val blockEntity = pos.nmsBlockEntity as BrewingStandBlockEntity
        
        val bottlesInventory = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(0, 3)))
        val ingredientInventory = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(3, 4)))
        val fuelInventory = NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents.subList(4, 5)))
        
        val inventories = CubeFaceMap(
            up = ingredientInventory,
            north = fuelInventory,
            east = fuelInventory,
            south = fuelInventory,
            west = fuelInventory,
            down = bottlesInventory
        )
        
        itemHolder = StaticVanillaItemHolder(storedValue("itemHolder", ::Compound), inventories)
        
        super.handleEnable()
    }
    
}