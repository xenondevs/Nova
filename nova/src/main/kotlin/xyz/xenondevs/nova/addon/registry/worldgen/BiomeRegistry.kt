@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder

@Deprecated(REGISTRIES_DEPRECATION)
interface BiomeRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun biomeInjection(name: String, biomeInjection: BiomeInjectionBuilder.() -> Unit) =
        addon.biomeInjection(name, biomeInjection)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun biome(name: String, biome: BiomeBuilder.() -> Unit): ResourceKey<Biome> =
        addon.biome(name, biome)
}