package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.entity.HopperBlockEntity
import org.bukkit.block.BlockFace
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.DefaultItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaHopperTileEntity(type: Type, pos: BlockPos, data: Compound) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        DefaultItemHolder.tryConvertLegacy(this)?.let { storeData("itemHolder", it) } // legacy conversion
        
        val facing = pos.nmsBlockState.getValue(HopperBlock.FACING).blockFace
        val inventory = NetworkedNMSInventory(SimpleItemStackContainer((pos.nmsBlockEntity as HopperBlockEntity).contents))
        itemHolder = StaticVanillaItemHolder(
            storedValue("itemHolder", ::Compound),
            CUBE_FACES.associateWithTo(enumMap()) { inventory }
        ) {
            val map = CUBE_FACES.associateWithTo(enumMap()) { NetworkConnectionType.NONE }
            map[BlockFace.UP] = NetworkConnectionType.INSERT
            map[facing] = NetworkConnectionType.EXTRACT
            map
        }
        
        super.handleEnable()
    }
    
}