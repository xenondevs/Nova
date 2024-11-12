package xyz.xenondevs.nova.patch.impl.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ScheduledTickAccess
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Orientation
import org.bukkit.block.data.BlockData
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.patch.MultiTransformer
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

private val BLOCK_BEHAVIOR_LOOKUP = MethodHandles
    .privateLookupIn(BlockBehaviour::class.java, MethodHandles.lookup())

private val BLOCK_BEHAVIOR_NEIGHBOR_CHANGED = BLOCK_BEHAVIOR_LOOKUP.findVirtual(
    BlockBehaviour::class.java,
    "neighborChanged",
    MethodType.methodType(
        Void.TYPE,
        BlockState::class.java,
        Level::class.java,
        BlockPos::class.java,
        Block::class.java,
        Orientation::class.java,
        Boolean::class.java
    )
)

private val BLOCK_BEHAVIOR_UPDATE_SHAPE = BLOCK_BEHAVIOR_LOOKUP.findVirtual(
    BlockBehaviour::class.java,
    "updateShape",
    MethodType.methodType(
        BlockState::class.java,
        BlockState::class.java,
        LevelReader::class.java,
        ScheduledTickAccess::class.java,
        BlockPos::class.java,
        Direction::class.java,
        BlockPos::class.java,
        BlockState::class.java,
        RandomSource::class.java
    )
)

private val BLOCK_BEHAVIOR_TICK = BLOCK_BEHAVIOR_LOOKUP.findVirtual(
    BlockBehaviour::class.java,
    "tick",
    MethodType.methodType(
        Void.TYPE,
        BlockState::class.java,
        ServerLevel::class.java,
        BlockPos::class.java,
        RandomSource::class.java
    )
)

private val BLOCK_BEHAVIOR_ENTITY_INSIDE = BLOCK_BEHAVIOR_LOOKUP.findVirtual(
    BlockBehaviour::class.java,
    "entityInside",
    MethodType.methodType(
        Void.TYPE,
        BlockState::class.java,
        Level::class.java,
        BlockPos::class.java,
        Entity::class.java
    )
)

internal object BlockBehaviorPatches : MultiTransformer(BlockStateBase::class) {
    
    override fun transform() {
        VirtualClassPath[BlockStateBase::handleNeighborChanged].delegateStatic(::handleNeighborChanged)
        VirtualClassPath[BlockStateBase::updateShape].delegateStatic(::updateShape)
        VirtualClassPath[BlockStateBase::tick].delegateStatic(::tick)
        VirtualClassPath[BlockStateBase::entityInside].delegateStatic(::entityInside)
    }
    
    @JvmStatic
    fun handleNeighborChanged(thisRef: BlockStateBase, level: Level, pos: BlockPos, sourceBlock: Block, wireOrientation: Orientation?, notify: Boolean) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleNeighborChanged(novaPos, novaState)
            } catch (e: Exception) {
                LOGGER.error("Failed to handle neighbor change for $novaState at $novaPos", e)
            }
        } else {
            BLOCK_BEHAVIOR_NEIGHBOR_CHANGED.invoke(thisRef.block, thisRef, level, pos, sourceBlock, wireOrientation, notify)
        }
    }
    
    @JvmStatic
    fun updateShape(thisRef: BlockStateBase, level: LevelReader, tickView: ScheduledTickAccess, pos: BlockPos, direction: Direction, neighborPos: BlockPos, neighborState: BlockState, random: RandomSource): BlockState {
        if (level is ServerLevel) { // fixme: needs to support WorldGenRegion
            val novaPos = pos.toNovaPos(level.world)
            val novaState = WorldDataManager.getBlockState(novaPos)
            if (novaState != null) {
                try {
                    val newState = novaState.block.updateShape(novaPos, novaState, neighborPos.toNovaPos(level.world))
                    if (newState != novaState) {
                        WorldDataManager.setBlockState(novaPos, newState)
                        return when (val info = newState.modelProvider.info) {
                            is BackingStateConfig -> info.vanillaBlockState
                            is BlockData -> info.nmsBlockState
                            is DisplayEntityBlockModelData -> {
                                novaState.modelProvider.unload(novaPos)
                                newState.modelProvider.load(novaPos)
                                info.hitboxType
                            }
                            
                            else -> throw UnsupportedOperationException()
                        }
                    }
                    
                    return thisRef as BlockState
                } catch (e: Exception) {
                    LOGGER.error("Failed to update shape for $novaState at $novaPos", e)
                }
            }
        }
        
        return BLOCK_BEHAVIOR_UPDATE_SHAPE.invoke(thisRef.block, thisRef, level, tickView, pos, direction, neighborPos, neighborState, random) as BlockState
    }
    
    @JvmStatic
    fun tick(thisRef: BlockStateBase, level: Level, pos: BlockPos, random: RandomSource) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleScheduledTick(novaPos, novaState)
            } catch (e: Exception) {
                LOGGER.error("Failed to handle vanilla scheduled tick for $novaState at $pos", e)
            }
        } else {
            BLOCK_BEHAVIOR_TICK.invoke(thisRef.block, thisRef, level, pos, random)
        }
    }
    
    @JvmStatic
    fun entityInside(thisRef: BlockStateBase, level: Level, pos: BlockPos, entity: Entity) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            try {
                novaState.block.handleEntityInside(novaPos, novaState, entity.bukkitEntity)
            } catch (e: Exception) {
                LOGGER.error("Failed to handle entity inside for $novaState at $novaPos", e)
            }
        } else {
            BLOCK_BEHAVIOR_ENTITY_INSIDE.invoke(thisRef.block, thisRef, level, pos, entity)
        }
    }
    
}