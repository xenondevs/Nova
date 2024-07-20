package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.format.WorldDataManager
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

private val BLOCK_BEHAVIOR_NEIGHBOR_CHANGED = MethodHandles
    .privateLookupIn(BlockBehaviour::class.java, MethodHandles.lookup())
    .findVirtual(
        BlockBehaviour::class.java,
        "neighborChanged",
        MethodType.methodType(
            Void.TYPE,
            BlockState::class.java,
            Level::class.java,
            BlockPos::class.java,
            Block::class.java,
            BlockPos::class.java,
            Boolean::class.java
        )
    )

internal object BlockUpdatesPatch : ClassTransformer(BlockStateBase::class) {
    
    override fun transform() {
        BlockStateBase::handleNeighborChanged.replaceWith(::handleNeighborChanged)
    }
    
    @JvmStatic
    fun handleNeighborChanged(thisRef: BlockStateBase, level: Level, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            novaState.block.handleNeighborChanged(novaPos, novaState, sourcePos.toNovaPos(level.world))
        } else {
            BLOCK_BEHAVIOR_NEIGHBOR_CHANGED.invoke(thisRef.block, thisRef, level, pos, sourceBlock, sourcePos, notify)
        }
    }
    
}