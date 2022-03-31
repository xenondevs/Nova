package xyz.xenondevs.nova.world.block.context

import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.BlockPos
import java.util.*

data class BlockPlaceContext(
    val pos: BlockPos,
    val source: Any?,
    val ownerUUID: UUID,
    val sourceLocation: Location?,
    val placedOn: BlockPos,
    val placedOnFace: BlockFace,
    val item: ItemStack?
) {
    
    init {
        require(item?.type?.isAir != true) { "empty item stacks are not allowed" }
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
    }
    
}