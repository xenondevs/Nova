package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils

object FeatureRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(Registries.FEATURE, Registries.CONFIGURED_FEATURE, Registries.PLACED_FEATURE)
    
    private val featureTypes = Object2ObjectOpenHashMap<NamespacedId, Feature<*>>()
    private val configuredFeatures = Object2ObjectOpenHashMap<NamespacedId, ConfiguredFeature<*, *>>()
    private val placedFeatures = Object2ObjectOpenHashMap<NamespacedId, PlacedFeature>()
    
    fun <FC : FeatureConfiguration> registerFeatureType(addon: Addon, name: String, feature: Feature<FC>) {
        val id = NamespacedId(addon, name)
        require(id !in featureTypes) { "Duplicate feature type $id" }
        featureTypes[id] = feature
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
    }
    
}