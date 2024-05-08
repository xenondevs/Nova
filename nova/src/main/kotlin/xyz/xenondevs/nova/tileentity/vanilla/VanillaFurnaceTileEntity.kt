package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import org.bukkit.block.Furnace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedRangedBukkitInventory
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
            getInventories(pos.block.state as Furnace)
        )
    }
    
    private fun getInventories(furnace: Furnace): EnumMap<BlockFace, NetworkedInventory> {
        val bukkitInventory = furnace.inventory
        val inputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 0)
        val fuelInventory = NetworkedRangedBukkitInventory(bukkitInventory, 1)
        val outputInventory = NetworkedRangedBukkitInventory(bukkitInventory, 2)
        
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { fuelInventory }
        inventories[BlockFace.UP] = inputInventory
        inventories[BlockFace.DOWN] = outputInventory
        
        return inventories
    }
    
}