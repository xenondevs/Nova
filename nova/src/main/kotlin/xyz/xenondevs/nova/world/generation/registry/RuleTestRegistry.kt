package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.set
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.world.generation.ruletest.MaterialMatchTestType

object RuleTestRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {

    override val neededRegistries get() = setOf(Registries.RULE_TEST)
    
    private val ruleTestTypes = Object2ObjectOpenHashMap<NamespacedId, RuleTestType<*>>()
    
    fun <T : RuleTest> registerRuleTestType(addon: Addon, name: String, ruleTestType: RuleTestType<T>) {
        val id = NamespacedId(addon, name)
        require(id !in ruleTestTypes) { "Duplicate rule test type $id" }
        ruleTestTypes[id] = ruleTestType
    }
    
    override fun register() {
        registerAll(Registries.RULE_TEST, ruleTestTypes)
    }
    
    override fun registerDefaults() {
        ruleTestTypes["nova", "material_match"] = MaterialMatchTestType
    }
    
}