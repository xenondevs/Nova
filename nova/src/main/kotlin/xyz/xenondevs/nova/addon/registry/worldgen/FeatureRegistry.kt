package xyz.xenondevs.nova.addon.registry.worldgen

import com.mojang.serialization.Codec
import net.minecraft.core.Holder
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
    fun placedFeature(name: String, placedFeature: PlacedFeatureBuilder.() -> Unit): PlacedFeature =
        PlacedFeatureBuilder(ResourceLocation(addon, name)).apply(placedFeature).register()
    
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
    fun <FC : FeatureConfiguration, F : Feature<FC>> registerConfiguredFeature(name: String, feature: F, config: FC): ConfiguredFeature<FC, F> =
        registerConfiguredFeature(name, ConfiguredFeature(feature, config))
    
    @ExperimentalWorldGen
    fun registerPlacedFeature(name: String, placedFeature: PlacedFeature): PlacedFeature {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.PLACED_FEATURE[id] = placedFeature
        return placedFeature
    }
    
    @ExperimentalWorldGen
    fun registerPlacedFeature(name: String, config: ConfiguredFeature<*, *>, vararg modifiers: PlacementModifier): PlacedFeature =
        registerPlacedFeature(name, PlacedFeature(Holder.direct(config), modifiers.toList()))
    
    @ExperimentalWorldGen
    fun registerPlacedFeature(name: String, config: ConfiguredFeature<*, *>, modifiers: List<PlacementModifier>): PlacedFeature =
        registerPlacedFeature(name, PlacedFeature(Holder.direct(config), modifiers))
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> registerPlacementModifierType(name: String, placementModifierType: PlacementModifierType<P>): PlacementModifierType<P> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.PLACEMENT_MODIFIER_TYPE[id] = placementModifierType
        return placementModifierType
    }
    
    @ExperimentalWorldGen
    fun <P : PlacementModifier> registerPlacementModifierType(name: String, codec: Codec<P>): PlacementModifierType<P> =
        registerPlacementModifierType(name) { codec }
    
}