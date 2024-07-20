package xyz.xenondevs.nova.world.generation

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ruletest.MaterialMatchTestType

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultWorldGenTypes {
    
    @ExperimentalWorldGen
    val MATERIAL_MATCH_TEST_TYPE = registerRuleTestType("material_match", MaterialMatchTestType)
    
    private fun registerRuleTestType(id: String, type: RuleTestType<*>): RuleTestType<*> {
        VanillaRegistries.RULE_TEST[ResourceLocation.fromNamespaceAndPath("nova", id)] = type
        return type
    }
    
}