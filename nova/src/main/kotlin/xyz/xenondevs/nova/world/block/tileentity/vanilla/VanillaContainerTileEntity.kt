package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
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
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedShulkerBoxInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaContainerTileEntity internal constructor(
    type: Type,
    pos: BlockPos,
    data: Compound
) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        val container = pos.nmsBlockEntity as BaseContainerBlockEntity
        val inventory = getInventory(container)
        val inventories = CUBE_FACES.associateWithTo(enumMap<BlockFace, NetworkedInventory>()) { inventory }
        
        DefaultItemHolder.tryConvertLegacy(this)?.let { storeData("itemHolder", it) } // legacy conversion
        itemHolder = StaticVanillaItemHolder(storedValue("itemHolder", ::Compound), inventories)
        
        super.handleEnable()
    }
    
    private fun getInventory(blockEntity: BaseContainerBlockEntity): NetworkedInventory {
        if (blockEntity is ShulkerBoxBlockEntity)
            return NetworkedShulkerBoxInventory(SimpleItemStackContainer(blockEntity.contents))
        return NetworkedNMSInventory(SimpleItemStackContainer(blockEntity.contents))
    }
    
}