package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import xyz.xenondevs.nova.data.world.block.state.VanillaTileEntityState
import xyz.xenondevs.nova.tileentity.network.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedShulkerBoxInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.emptyEnumMap

class VanillaContainerTileEntity internal constructor(blockState: VanillaTileEntityState) : ItemStorageVanillaTileEntity(blockState) {
    
    override val itemHolder: StaticVanillaItemHolder
    
    init {
        val container = blockState.pos.block.state as Container
        
        val inventory = if (container is ShulkerBox) NetworkedShulkerBoxInventory(container.inventory) else NetworkedBukkitInventory(container.inventory)
        val inventories = CUBE_FACES.associateWithTo(emptyEnumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(this, inventories)
    }
    
}