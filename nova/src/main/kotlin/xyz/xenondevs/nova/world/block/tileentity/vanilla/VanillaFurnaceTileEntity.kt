package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaFurnaceTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        itemHolder = StaticVanillaItemHolder(
            storedValue("itemHolder", ::Compound),
            getInventories(pos.nmsBlockEntity as AbstractFurnaceBlockEntity)
        )
        
        super.handleEnable()
    }
    
    private fun getInventories(furnace: AbstractFurnaceBlockEntity): CubeFaceMap<NetworkedInventory> {
        val contents = furnace.contents
        val inputInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(0, 1)))
        val fuelInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(1, 2)))
        val outputInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(2, 3)))
        
        return CubeFaceMap(
            up = inputInventory,
            north = fuelInventory,
            east = fuelInventory,
            south = fuelInventory,
            west = fuelInventory,
            down = outputInventory
        )
    }
    
}