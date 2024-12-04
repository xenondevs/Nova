package xyz.xenondevs.nova.world.generation

import net.kyori.adventure.key.Key
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.patch.impl.registry.set
import xyz.xenondevs.nova.world.generation.ruletest.NovaBlockMatchTestType

@InternalInit(stage = InternalInitStage.PRE_WORLD)
object DefaultWorldGenTypes {
    
    @ExperimentalWorldGen
    val BLOCK_MATCH_TEST_TYPE = registerRuleTestType("block_match", NovaBlockMatchTestType)
    
    private fun registerRuleTestType(id: String, type: RuleTestType<*>): RuleTestType<*> {
        Registries.RULE_TEST[Key.key("nova", id)] = type
        return type
    }
    
}