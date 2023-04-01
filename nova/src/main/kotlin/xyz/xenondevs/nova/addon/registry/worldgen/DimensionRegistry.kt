package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.world.level.dimension.DimensionType
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.DimensionTypeBuilder

interface DimensionRegistry : AddonGetter {
    
    @ExperimentalWorldGen
    fun dimensionType(name: String): DimensionTypeBuilder {
        val id = ResourceLocation(addon, name)
        return DimensionTypeBuilder(id)
    }
    
    @ExperimentalWorldGen
    fun registerDimensionType(name: String, dimensionType: DimensionType): DimensionType {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.DIMENSION_TYPE[id] = dimensionType
        return dimensionType
    }
    
}