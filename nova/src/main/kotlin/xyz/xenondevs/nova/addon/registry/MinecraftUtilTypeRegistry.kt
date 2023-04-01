package xyz.xenondevs.nova.addon.registry

import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set

interface MinecraftUtilTypeRegistry : AddonGetter {
    
    fun <P : RuleTest> registerRuleTestType(name: String, ruleTestType: RuleTestType<P>): RuleTestType<P> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.RULE_TEST[id] = ruleTestType
        return ruleTestType
    }
    
}