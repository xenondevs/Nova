package xyz.xenondevs.nova.world.generation.ruletest

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
class MaterialMatchTest(val material: NovaBlock) : NovaMaterialTest() {
    
    override fun test(material: NovaBlock, level: Level, pos: BlockPos, state: BlockState, random: RandomSource): Boolean {
        return material == this.material
    }
    
    override fun getType(): RuleTestType<*> {
        return MaterialMatchTestType
    }
    
}

@ExperimentalWorldGen
object MaterialMatchTestType : RuleTestType<MaterialMatchTest> {
    
    private val CODEC: Codec<MaterialMatchTest> =
        NovaBlock.CODEC
            .fieldOf("material")
            .xmap(::MaterialMatchTest, MaterialMatchTest::material)
            .codec()
            .stable()
    
    override fun codec() = CODEC
    
}