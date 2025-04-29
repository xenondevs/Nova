package xyz.xenondevs.nova.addon.registry

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION

@Deprecated(REGISTRIES_DEPRECATION)
interface MinecraftUtilTypeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : RuleTest> registerRuleTestType(name: String, ruleTestType: RuleTestType<T>): RuleTestType<T> =
        addon.registerRuleTestType(name, ruleTestType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    fun <T : RuleTest> registerRuleTestType(name: String, codec: MapCodec<T>): RuleTestType<T> =
        addon.registerRuleTestType(name, codec)
    
}