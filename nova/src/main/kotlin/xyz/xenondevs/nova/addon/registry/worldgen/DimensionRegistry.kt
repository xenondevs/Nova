package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.dimension.DimensionType
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.DimensionTypeBuilder

@Deprecated(REGISTRIES_DEPRECATION)
interface DimensionRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun dimensionType(name: String, dimensionType: DimensionTypeBuilder.() -> Unit): ResourceKey<DimensionType> =
        addon.dimensionType(name, dimensionType)
}