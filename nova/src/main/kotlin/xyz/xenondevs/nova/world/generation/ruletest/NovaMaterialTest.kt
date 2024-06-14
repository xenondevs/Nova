package xyz.xenondevs.nova.world.generation.ruletest

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import net.minecraft.world.level.block.state.BlockState as MojangState

@ExperimentalWorldGen
abstract class NovaMaterialTest : NovaRuleTest() {
    
    final override fun test(level: Level, pos: BlockPos, state: MojangState, random: RandomSource): Boolean {
        val blockState = WorldDataManager.getBlockState(pos.toNovaPos(level.world)) ?: return false
        return test(blockState.block, level, pos, state, random)
    }
    
    abstract fun test(material: NovaBlock, level: Level, pos: BlockPos, state: MojangState, random: RandomSource): Boolean
    
}