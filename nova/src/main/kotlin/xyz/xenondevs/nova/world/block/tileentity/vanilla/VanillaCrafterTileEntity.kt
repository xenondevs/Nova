package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.CrafterBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedCrafterInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaCrafterTileEntity(
    type: Type,
    pos: BlockPos, 
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        val blockEntity = pos.nmsBlockEntity as CrafterBlockEntity
        val inventory = NetworkedCrafterInventory(SimpleItemStackContainer(blockEntity.contents))
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { inventory }
        itemHolder = StaticVanillaItemHolder(storedValue("itemHolder", ::Compound), inventories)
        
        super.handleEnable()
    }
    
}