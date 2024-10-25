package xyz.xenondevs.nova.addon.registry

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set

interface MinecraftUtilTypeRegistry : AddonHolder {
    
    fun <T : RuleTest> registerRuleTestType(name: String, ruleTestType: RuleTestType<T>): RuleTestType<T> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.RULE_TEST[id] = ruleTestType
        return ruleTestType
    }
    
    fun <T : RuleTest> registerRuleTestType(name: String, codec: MapCodec<T>): RuleTestType<T> =
        registerRuleTestType(name) { codec }
    
}