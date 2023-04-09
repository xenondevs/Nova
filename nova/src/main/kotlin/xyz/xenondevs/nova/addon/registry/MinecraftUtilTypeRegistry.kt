package xyz.xenondevs.nova.addon.registry

import com.mojang.serialization.Codec
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
    
    fun <T : RuleTest> registerRuleTestType(name: String, codec: Codec<T>): RuleTestType<T> =
        registerRuleTestType(name) { codec }
    
}