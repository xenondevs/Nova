@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.addon.registry.worldgen

import com.mojang.serialization.MapCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.PlacedFeatureBuilder

@Deprecated(REGISTRIES_DEPRECATION)
@RegistryElementBuilderDsl
interface FeatureRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun placedFeature(name: String, placedFeature: PlacedFeatureBuilder.() -> Unit): ResourceKey<PlacedFeature> =
        addon.placedFeature(name, placedFeature)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration, F : Feature<FC>> configuredFeature(name: String, feature: F, config: FC): ResourceKey<ConfiguredFeature<*, *>> =
        addon.configuredFeature(name, feature, config)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <F : ConfiguredFeature<*, *>> configuredFeature(name: String, configuredFeature: F): ResourceKey<ConfiguredFeature<*, *>> =
        addon.configuredFeature(name, configuredFeature)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun feature(name: String, feature: Feature<*>): ResourceKey<Feature<*>> =
        addon.feature(name, feature)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> =
        addon.placementModifierType(name, placementModifierType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, codec: MapCodec<P>): PlacementModifierType<P> =
        addon.placementModifierType(name, codec)
    
}