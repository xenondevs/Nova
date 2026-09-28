package xyz.xenondevs.nova.world.block.tileentity.vanilla

import net.minecraft.core.BlockPos
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftChunk
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.context.intention.ImplicitIntentions
import xyz.xenondevs.nova.util.exec
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.nmsBlockEntity
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkNodeProvider
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkNodeSnapshot
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import java.util.*
import java.util.function.BooleanSupplier

internal object VanillaTileEntityManager {
    
    private val blockPlaceContext = ScopedValue.newInstance<BlockPlaceContext>()
    
    @JvmStatic
    fun withBlockPlacement(context: BlockPlaceContext, operation: BooleanSupplier): Boolean =
        ScopedValue.where(blockPlaceContext, context).exec(operation::getAsBoolean)
    
    internal fun getPlacementOwner(pos: BlockPos): UUID? {
        val context = blockPlaceContext.getOrNull()
            ?.takeIf { it.clickedPos == pos }
            ?: return null
        val implicitContext = ImplicitIntentions.BLOCK_PLACE.getOrNull()
        return if (implicitContext != null)
            implicitContext[BlockPlace.RESPONSIBLE_PLAYER]?.uniqueId
        else context.player?.uuid
    }
    
    @JvmStatic
    fun handleBlockStateChange(
        oldState: BlockState,
        newState: BlockState,
        oldBlockEntity: BlockEntity?,
        newBlockEntity: BlockEntity?
    ) {
        if (oldBlockEntity !== newBlockEntity) {
            val oldTileEntity = oldBlockEntity?.vanillaTileEntity
            if (oldTileEntity is NetworkEndPoint) {
                if (oldState.block !== newState.block && oldTileEntity is ItemStorageVanillaTileEntity)
                    oldTileEntity.handleBreak()
                NetworkManager.queueRemoveEndPoint(oldTileEntity)
            }
            
            val newTileEntity = newBlockEntity?.let(VanillaTileEntity::of)
            if (newTileEntity is NetworkEndPoint)
                NetworkManager.queueAddEndPoint(newTileEntity)
        } else if (
            newBlockEntity is ChestBlockEntity &&
            VanillaChestTileEntity.isLinkStateChanged(oldState, newState)
        ) {
            (VanillaTileEntity.of(newBlockEntity) as VanillaChestTileEntity).refreshLink()
        } else if (newBlockEntity is VanillaCauldronBlockEntity) {
            (VanillaTileEntity.of(newBlockEntity) as VanillaCauldronTileEntity).handleBlockStateChange(newState)
        }
    }
    
    @JvmStatic
    fun handleBlockEntityRemoved(blockEntity: BlockEntity) {
        (blockEntity.vanillaTileEntity as? VanillaChestTileEntity)?.handleRemoved()
    }
    
    @JvmStatic
    fun handleBlockEntityAdded(blockEntity: BlockEntity) {
        (blockEntity.vanillaTileEntity as? VanillaChestTileEntity)?.refreshLink()
    }
    
}