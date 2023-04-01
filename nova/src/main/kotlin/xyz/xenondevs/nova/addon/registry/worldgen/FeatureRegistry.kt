package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.FeatureType
import xyz.xenondevs.nova.world.generation.builder.PlacedFeatureBuilder

interface FeatureRegistry : AddonGetter {
    
    @ExperimentalWorldGen
    fun placedFeature(name: String): PlacedFeatureBuilder {
        val id = ResourceLocation(addon, name)
        return PlacedFeatureBuilder(id)
    }
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, configuredFeature: ConfiguredFeature<*, *>) =
        placedFeature(name).configuredFeature(configuredFeature)
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, configuredFeature: Holder<ConfiguredFeature<*, *>>) =
        placedFeature(name).configuredFeature(configuredFeature)
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, configuredFeatureId: ResourceLocation) =
        placedFeature(name).configuredFeature(configuredFeatureId)
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, configuredFeatureKey: ResourceKey<ConfiguredFeature<*, *>>) =
        placedFeature(name).configuredFeature(configuredFeatureKey)
    
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration> registerFeatureType(name: String, feature: FeatureType<FC>): FeatureType<FC> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.FEATURE[id] = feature
        return feature
    }
    
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration, F : Feature<FC>> registerConfiguredFeature(name: String, configuredFeature: ConfiguredFeature<FC, F>): ConfiguredFeature<FC, F> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.CONFIGURED_FEATURE[id] = configuredFeature
        return configuredFeature
    }
    
    @ExperimentalWorldGen
    fun registerPlacedFeature(name: String, placedFeature: PlacedFeature): PlacedFeature {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.PLACED_FEATURE[id] = placedFeature
        return placedFeature
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> registerPlacementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.PLACEMENT_MODIFIER_TYPE[id] = placementModifierType
        return placementModifierType
    }
    
}