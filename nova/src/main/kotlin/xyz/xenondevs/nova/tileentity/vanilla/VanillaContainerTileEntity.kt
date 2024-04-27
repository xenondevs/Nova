package xyz.xenondevs.nova.tileentity.vanilla

import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedBukkitInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedShulkerBoxInventory
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos

internal class VanillaContainerTileEntity internal constructor(
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(pos, data) {
    
    override val type = Type.CONTAINER
    override val itemHolder: StaticVanillaItemHolder
    
    init {
        val container = pos.block.state as Container
        
        val inventory = if (container is ShulkerBox) NetworkedShulkerBoxInventory(container.inventory) else NetworkedBukkitInventory(container.inventory)
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(storedValue("itemHolder", ::Compound).get(), inventories) // TODO: legacy support
    }
    
}