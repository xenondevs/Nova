package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import org.bukkit.block.Furnace
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedRangedBukkitInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.emptyEnumMap
import java.util.*

internal class VanillaFurnaceTileEntity internal constructor(blockState: VanillaTileEntityState) : ItemStorageVanillaTileEntity(blockState) {
    
    override val type = Type.FURNACE
    override val itemHolder = StaticVanillaItemHolder(this, getInventories(block.state as Furnace))
    
    private fun getInventories(furnace: Furnace): EnumMap<BlockFace, NetworkedInventory> {
        val bukkitInventory = furnace.inventory
        val inputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 0)
        val fuelInventory = NetworkedRangedBukkitInventory(bukkitInventory, 1)
        val outputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 2)
        
        val inventories = CUBE_FACES.associateWithTo(emptyEnumMap<BlockFace, NetworkedInventory>()) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
        
        return inventories
    }
    
}