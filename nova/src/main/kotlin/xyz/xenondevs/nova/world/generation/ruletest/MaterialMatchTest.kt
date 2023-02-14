package xyz.xenondevs.nova.world.generation.ruletest

import com.mojang.serialization.Codec
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.material.BlockNovaMaterial

class MaterialMatchTest(val material: BlockNovaMaterial) : NovaMaterialTest() {
    
    override fun test(material: BlockNovaMaterial, level: Level, pos: BlockPos, state: BlockState, random: RandomSource): Boolean {
        return material == this.material
    }
    
    override fun getType(): RuleTestType<*> {
        return MaterialMatchTestType
    }
    
}

object MaterialMatchTestType : RuleTestType<MaterialMatchTest> {
    
    private val CODEC: Codec<MaterialMatchTest> =
        BlockNovaMaterial.CODEC
            .fieldOf("material")
            .xmap(::MaterialMatchTest, MaterialMatchTest::material)
            .codec()
    
    override fun codec() = CODEC
    
}