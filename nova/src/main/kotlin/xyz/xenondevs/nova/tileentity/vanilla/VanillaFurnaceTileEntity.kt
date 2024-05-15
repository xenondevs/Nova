package xyz.xenondevs.nova.tileentity.vanilla

import net.minecraft.world.level.block.entity.FurnaceBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.vanilla.SingleSlotMojangStackContainer
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import java.util.*

internal class VanillaFurnaceTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(pos, data) {
    
    override val type = Type.FURNACE
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        itemHolder = StaticVanillaItemHolder(
            storedValue("itemHolder", ::Compound), // TODO: legacy support
            getInventories(pos.nmsBlockEntity as FurnaceBlockEntity)
        )
    }
    
    private fun getInventories(furnace: FurnaceBlockEntity): EnumMap<BlockFace, NetworkedInventory> {
        val contents = furnace.contents
        val inputInventory = NetworkedNMSInventory(SingleSlotMojangStackContainer(contents, 0))
        val fuelInventory = NetworkedNMSInventory(SingleSlotMojangStackContainer(contents, 1))
        val outputInventory = NetworkedNMSInventory(SingleSlotMojangStackContainer(contents, 2))
        
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
        
        return inventories
    }
    
}