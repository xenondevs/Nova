package xyz.xenondevs.nova.addon.registry.worldgen

import com.mojang.serialization.MapCodec
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import net.minecraft.world.level.levelgen.feature.Feature
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import net.minecraft.world.level.levelgen.placement.PlacementModifier
import net.minecraft.world.level.levelgen.placement.PlacementModifierType
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.patch.impl.registry.set
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.registry.buildRegistryElementLater
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.PlacedFeatureBuilder

@RegistryElementBuilderDsl
interface FeatureRegistry : AddonGetter {
    
    @ExperimentalWorldGen
    fun placedFeature(name: String, placedFeature: PlacedFeatureBuilder.() -> Unit): ResourceKey<PlacedFeature> =
        buildRegistryElementLater(addon, name, Registries.PLACED_FEATURE, ::PlacedFeatureBuilder, placedFeature)
    
    @ExperimentalWorldGen
    fun <FC : FeatureConfiguration, F : Feature<FC>> configuredFeature(name: String, feature: F, config: FC): ResourceKey<ConfiguredFeature<*, *>> =
        configuredFeature(name, ConfiguredFeature(feature, config))
    
    @ExperimentalWorldGen
    fun <F : ConfiguredFeature<*, *>> configuredFeature(name: String, configuredFeature: F): ResourceKey<ConfiguredFeature<*, *>> {
        val key = ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation(addon, name))
        Registries.CONFIGURED_FEATURE[key] = configuredFeature
        return key
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> {
        val id = ResourceLocation(addon, name)
        Registries.PLACEMENT_MODIFIER_TYPE[id] = placementModifierType
        return placementModifierType
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> placementModifierType(name: String, codec: MapCodec<P>): PlacementModifierType<P> =
        placementModifierType(name) { codec }
    
}