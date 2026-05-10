package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.entity.HopperBlockEntity
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.util.CubeFaceMap
import xyz.xenondevs.nova.util.blockFace
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.holder.StaticVanillaItemHolder
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.NetworkedNMSInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.vanilla.SimpleItemStackContainer

internal class VanillaHopperTileEntity(type: Type, pos: BlockPos, data: Compound) : ItemStorageVanillaTileEntity(type, pos, data) {
    
    override lateinit var itemHolder: ItemHolder
    
    override fun handleEnable() {
        val facing = pos.nmsBlockState.getValue(HopperBlock.FACING).blockFace
        val inventory = NetworkedNMSInventory(SimpleItemStackContainer((pos.nmsBlockEntity as HopperBlockEntity).contents))
        itemHolder = StaticVanillaItemHolder(
            storedValue("itemHolder", ::Compound),
            CubeFaceMap(inventory),
            CubeFaceMap(
                up = NetworkConnectionType.INSERT,
                north = NetworkConnectionType.NONE,
                east = NetworkConnectionType.NONE,
                south = NetworkConnectionType.NONE,
                west = NetworkConnectionType.NONE,
                down = NetworkConnectionType.NONE
            ).with(facing, NetworkConnectionType.EXTRACT)
        )
        
        super.handleEnable()
    }
    
}