package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.RegistryAccess
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object FeatureRegistry : WorldGenRegistry() {
    
    override val neededRegistries = setOf(Registries.FEATURE, Registries.CONFIGURED_FEATURE, Registries.PLACED_FEATURE)
    
    private val featureTypes = Object2ObjectOpenHashMap<NamespacedId, Feature<*>>()
    private val configuredFeatures = Object2ObjectOpenHashMap<NamespacedId, ConfiguredFeature<*, *>>()
    private val placedFeatures = Object2ObjectOpenHashMap<NamespacedId, PlacedFeature>()
    
    fun <FC : FeatureConfiguration> registerFeatureType(addon: Addon, name: String, feature: Feature<FC>) {
        val id = NamespacedId(addon, name)
        require(id !in featureTypes) { "Duplicate feature type $id" }
        featureTypes[id] = feature
    }
    
    fun <F : Feature<FC>, FC : FeatureConfiguration> registerConfigureFeature(addon: Addon, name: String, configuredFeature: ConfiguredFeature<FC, F>) {
        val id = NamespacedId(addon, name)
        require(id !in configuredFeatures) { "Duplicate configured feature $id" }
        configuredFeatures[id] = configuredFeature
    }
    
    fun registerPlacedFeature(addon: Addon, name: String, placedFeature: PlacedFeature) {
        val id = NamespacedId(addon, name)
        require(id !in placedFeatures) { "Duplicate placed feature $id" }
        placedFeatures[id] = placedFeature
    }
    
    override fun register(registryAccess: RegistryAccess) {
        registerAll(registryAccess, Registries.FEATURE, featureTypes)
        loadFiles("configured_feature", ConfiguredFeature.CODEC, configuredFeatures)
        registerAll(registryAccess, Registries.CONFIGURED_FEATURE, configuredFeatures)
        loadFiles("placed_feature", PlacedFeature.CODEC, placedFeatures)
        registerAll(registryAccess, Registries.PLACED_FEATURE, placedFeatures)
    }
    
}