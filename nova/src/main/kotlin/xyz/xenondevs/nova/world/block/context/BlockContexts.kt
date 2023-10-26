package xyz.xenondevs.nova.world.block.context

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.BlockState
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.UUIDUtils
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.pos
import java.util.*
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

private fun getSourceLocation(source: Any?): Location? =
    when (source) {
        is Entity -> source.location
        is BlockState -> source.pos.location
        is TileEntity -> source.location
        is Location -> source
        else -> null
    }

private fun getOwnerUUID(source: Any?): UUID? =
    when (source) {
        is Entity -> source.uniqueId
        is TileEntity -> source.ownerUUID
        is UUID -> source
        else -> null
    }

data class BlockPlaceContext(
    val pos: BlockPos,
    val item: ItemStack,
    val source: Any?,
    val sourceLocation: Location?,
    val ownerUUID: UUID?,
    val placedOn: BlockPos,
    val placedOnFace: BlockFace
) {
    
    init {
        require(ownerUUID != UUIDUtils.ZERO) { "Owner UUID must not be 0-0. Use null instead." }
        require(!item.type.isAir) { "empty item stacks are not allowed" }
    }
    
    internal companion object {
        
        fun forAPI(location: Location, material: INovaBlock, source: Any?): BlockPlaceContext {
            val pos = location.pos
            return BlockPlaceContext(
                pos,
                material.item?.createItemStack(1) ?: throw IllegalArgumentException("Block needs an item"),
                source,
                getSourceLocation(source) ?: location,
                getOwnerUUID(source),
                pos.add(0, -1, 0),
                BlockFace.UP
            )
        }
    
    }
    
}

data class BlockBreakContext(
    val pos: BlockPos,
    val source: Any? = null,
    val sourceLocation: Location? = null,
    val clickedFace: BlockFace? = null,
    val item: ItemStack? = null
) {
    
    init {
        require(item?.type?.isAir != true) { "empty item stacks are not allowed" }
    }
    
    internal companion object {
        
        fun forAPI(location: Location, source: Any?, tool: ItemStack?): BlockBreakContext {
            return BlockBreakContext(
                location.pos,
                source,
                getSourceLocation(source) ?: location,
                null,
                tool
            )
        }
        
    }
    
}

data class BlockInteractContext(
    val pos: BlockPos,
    val source: Any? = null,
    val sourceLocation: Location? = null,
    val clickedFace: BlockFace? = null,
    val item: ItemStack? = null,
    val hand: EquipmentSlot? = null
) {
    
    init {
        require(item?.type?.isAir != true) { "empty item stacks are not allowed" }
        require(hand == null || hand == EquipmentSlot.HAND || hand == EquipmentSlot.OFF_HAND) { "equipment slot is not a hand" }
    }
    
}