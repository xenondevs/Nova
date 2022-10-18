package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.data.BuiltinRegistries
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

object FeatureRegistry : WorldGenRegistry() {
    
    override val neededRegistries = setOf(Registry.FEATURE_REGISTRY, Registry.CONFIGURED_FEATURE_REGISTRY, Registry.PLACED_FEATURE_REGISTRY)
    
    private val featureTypes = Object2ObjectOpenHashMap<NamespacedId, Feature<*>>()
    private val configuredFeatures = Object2ObjectOpenHashMap<NamespacedId, ConfiguredFeature<*, *>>()
    private val placedFeatures = Object2ObjectOpenHashMap<NamespacedId, PlacedFeature>()
    
    override fun loadFiles() {
        configuredFeatures.putAll(loadFiles("configured_feature", ConfiguredFeature.CODEC))
        placedFeatures.putAll(loadFiles("placed_feature", PlacedFeature.CODEC))
    }
    
    override fun register(registryAccess: RegistryAccess) {
        val featureTypeRegistry = registryAccess.registry(Registry.FEATURE_REGISTRY).get()
        val configuredFeatureRegistry = registryAccess.registry(Registry.CONFIGURED_FEATURE_REGISTRY).get()
        val placedFeatureRegistry = registryAccess.registry(Registry.PLACED_FEATURE_REGISTRY).get()
        
        featureTypes.forEach { (id, featureType) -> BuiltinRegistries.registerExact(featureTypeRegistry, id.toString(":"), featureType) }
        configuredFeatures.forEach { (id, configuredFeature) -> BuiltinRegistries.registerExact(configuredFeatureRegistry, id.toString(":"), configuredFeature) }
        placedFeatures.forEach { (id, placedFeature) -> BuiltinRegistries.registerExact(placedFeatureRegistry, id.toString(":"), placedFeature) }
    }
    
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
    
}