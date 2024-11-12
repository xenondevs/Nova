package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.DimensionType
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.buildRegistryElementLater
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.DimensionTypeBuilder

interface DimensionRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun dimensionType(name: String, dimensionType: DimensionTypeBuilder.() -> Unit): ResourceKey<DimensionType> =
         buildRegistryElementLater(addon, name, Registries.DIMENSION_TYPE, ::DimensionTypeBuilder, dimensionType)
    
}