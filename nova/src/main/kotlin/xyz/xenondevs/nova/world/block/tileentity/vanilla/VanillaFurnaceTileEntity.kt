package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.DefaultItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer
import java.util.*

internal class VanillaFurnaceTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        DefaultItemHolder.tryConvertLegacy(this)?.let { storeData("itemHolder", it) } // legacy conversion
        itemHolder = StaticVanillaItemHolder(
            storedValue("itemHolder", ::Compound),
            getInventories(pos.nmsBlockEntity as AbstractFurnaceBlockEntity)
        )
        
        super.handleEnable()
    }
    
    private fun getInventories(furnace: AbstractFurnaceBlockEntity): EnumMap<BlockFace, NetworkedInventory> {
        val contents = furnace.contents
        val inputInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(0, 1)))
        val fuelInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(1, 2)))
        val outputInventory = NetworkedNMSInventory(SimpleItemStackContainer(contents.subList(2, 3)))
        
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
        
        return inventories
    }
    
}