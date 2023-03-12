package xyz.xenondevs.nova.world.generation.ruletest

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
abstract class NovaRuleTest : RuleTest() {
    
    final override fun test(state: BlockState, random: RandomSource): Boolean {
        throw UnsupportedOperationException("test(state: BlockState, random: RandomSource) is not supported in NovaRuleTest.")
    }
    
    abstract fun test(level: Level, pos: BlockPos, state: BlockState, random: RandomSource): Boolean

}