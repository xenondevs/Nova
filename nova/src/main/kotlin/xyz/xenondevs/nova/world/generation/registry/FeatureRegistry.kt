package xyz.xenondevs.nova.world.generation.registry

import com.mojang.serialization.Codec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.set
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.world.generation.ExperimentalLevelGen
import xyz.xenondevs.nova.world.generation.ruletest.MaterialMatchTestType

@ExperimentalLevelGen
object FeatureRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries
        get() = setOf(
            Registries.FEATURE, Registries.CONFIGURED_FEATURE, Registries.PLACED_FEATURE, Registries.RULE_TEST, Registries.PLACEMENT_MODIFIER_TYPE
        )
    
    private val featureTypes = Object2ObjectOpenHashMap<NamespacedId, Feature<*>>()
    private val configuredFeatures = Object2ObjectOpenHashMap<NamespacedId, ConfiguredFeature<*, *>>()
    private val placedFeatures = Object2ObjectOpenHashMap<NamespacedId, PlacedFeature>()
    private val ruleTestTypes = Object2ObjectOpenHashMap<NamespacedId, RuleTestType<*>>()
    private val placementModifierTypes = Object2ObjectOpenHashMap<NamespacedId, PlacementModifierType<*>>()
    
    fun <FC : FeatureConfiguration> registerFeatureType(addon: Addon, name: String, feature: Feature<FC>): Feature<FC> {
        val id = NamespacedId(addon, name)
        require(id !in featureTypes) { "Duplicate feature type $id" }
        featureTypes[id] = feature
        return feature
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <F : Feature<FC>, FC : FeatureConfiguration> registerConfiguredFeature(
        addon: Addon,
        name: String,
        configuredFeature: ConfiguredFeature<FC, F>
    ): Holder<ConfiguredFeature<FC, F>> {
        val id = NamespacedId(addon, name)
        require(id !in configuredFeatures) { "Duplicate configured feature $id" }
        configuredFeatures[id] = configuredFeature
        return getHolder(id, Registries.CONFIGURED_FEATURE) as Holder<ConfiguredFeature<FC, F>>
    }
    
    fun <F : Feature<FC>, FC : FeatureConfiguration> registerConfiguredFeature(addon: Addon, name: String, feature: F, config: FC) =
        registerConfiguredFeature(addon, name, ConfiguredFeature(feature, config))
    
    
    fun registerPlacedFeature(addon: Addon, name: String, placedFeature: PlacedFeature): Holder<PlacedFeature> {
        val id = NamespacedId(addon, name)
        require(id !in placedFeatures) { "Duplicate placed feature $id" }
        placedFeatures[id] = placedFeature
        return getHolder(id, Registries.PLACED_FEATURE)
    }
    
    fun <T : RuleTest> registerRuleTestType(addon: Addon, name: String, ruleTestType: RuleTestType<T>): RuleTestType<T> {
        val id = NamespacedId(addon, name)
        require(id !in ruleTestTypes) { "Duplicate rule test type $id" }
        ruleTestTypes[id] = ruleTestType
        return ruleTestType
    }
    
    fun <T : RuleTest> registerRuleTestType(addon: Addon, name: String, codec: Codec<T>): RuleTestType<T> =
        registerRuleTestType(addon, name) { codec }
    
    fun <T : PlacementModifier> registerPlacementModifierType(addon: Addon, name: String, placementModifierType: PlacementModifierType<T>): PlacementModifierType<T> {
        val id = NamespacedId(addon, name)
        require(id !in placementModifierTypes) { "Duplicate placement modifier type $id" }
        placementModifierTypes[id] = placementModifierType
        return placementModifierType
    }
    
    fun <T : PlacementModifier> registerPlacementModifierType(addon: Addon, name: String, codec: Codec<T>): PlacementModifierType<T> =
        registerPlacementModifierType(addon, name) { codec }
    
    @Suppress("UNCHECKED_CAST")
    fun <F : Feature<FC>, FC : FeatureConfiguration> registerPlacedFeature(
        addon: Addon,
        name: String,
        configuredFeature: Holder<ConfiguredFeature<FC, F>>,
        placement: List<PlacementModifier>
    ): Holder<PlacedFeature> {
        val placedFeature = PlacedFeature(configuredFeature as Holder<ConfiguredFeature<*, *>>, placement)
        return registerPlacedFeature(addon, name, placedFeature)
    }
    
    @Suppress("UNCHECKED_CAST")
    fun <F : Feature<FC>, FC : FeatureConfiguration> registerPlacedFeature(
        addon: Addon,
        name: String,
        configuredFeature: Holder<ConfiguredFeature<FC, F>>,
        vararg placement: PlacementModifier
    ): Holder<PlacedFeature> {
        val placedFeature = PlacedFeature(configuredFeature as Holder<ConfiguredFeature<*, *>>, placement.toList())
        return registerPlacedFeature(addon, name, placedFeature)
    }
    
    override fun register() {
        registerAll(Registries.FEATURE, featureTypes)
        loadFiles("configured_feature", ConfiguredFeature.CODEC, configuredFeatures)
        registerAll(Registries.CONFIGURED_FEATURE, configuredFeatures)
        loadFiles("placed_feature", PlacedFeature.CODEC, placedFeatures)
        registerAll(Registries.PLACED_FEATURE, placedFeatures)
        
        registerAll(Registries.RULE_TEST, ruleTestTypes)
        registerAll(Registries.PLACEMENT_MODIFIER_TYPE, placementModifierTypes)
    }
    
    override fun registerDefaults() {
        ruleTestTypes["nova", "material_match"] = MaterialMatchTestType
    }
    
}