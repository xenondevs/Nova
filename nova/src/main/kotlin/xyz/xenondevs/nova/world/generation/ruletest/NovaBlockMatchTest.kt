package xyz.xenondevs.nova.world.generation.ruletest

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.byNameCodec
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
class NovaBlockMatchTest(val block: NovaBlock) : NovaBlockTest() {
    
    override fun test(material: NovaBlock, level: Level, pos: BlockPos, state: BlockState, random: RandomSource): Boolean {
        return material == this.block
    }
    
    override fun getType(): RuleTestType<*> {
        return NovaBlockMatchTestType
    }
    
}

@ExperimentalWorldGen
object NovaBlockMatchTestType : RuleTestType<NovaBlockMatchTest> {
    
    private val CODEC: MapCodec<NovaBlockMatchTest> =
        NovaRegistries.BLOCK.byNameCodec()
            .fieldOf("block")
            .xmap(::NovaBlockMatchTest, NovaBlockMatchTest::block)
    
    override fun codec() = CODEC
    
}