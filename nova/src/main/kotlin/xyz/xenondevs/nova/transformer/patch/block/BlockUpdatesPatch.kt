@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.transformer.patch.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.transformer.ClassTransformer
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.format.WorldDataManager

internal object BlockUpdatesPatch : ClassTransformer(BlockStateBase::class) {
    
    override fun transform() {
        BlockStateBase::neighborChanged.replaceWith(::neighborChanged)
    }
    
    @JvmStatic
    fun neighborChanged(thisRef: BlockStateBase, level: Level, pos: BlockPos, sourceBlock: Block, sourcePos: BlockPos, notify: Boolean) {
        val novaPos = pos.toNovaPos(level.world)
        val novaState = WorldDataManager.getBlockState(novaPos)
        if (novaState != null) {
            novaState.block.handleNeighborChanged(novaPos, novaState, sourcePos.toNovaPos(level.world))
        } else {
            thisRef.block.neighborChanged(thisRef as BlockState, level, pos, sourceBlock, sourcePos, notify)
        }
    }
    
}