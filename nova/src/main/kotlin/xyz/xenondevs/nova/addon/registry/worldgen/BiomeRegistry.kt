package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.biome.Biome
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.buildRegistryElementLater
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder

interface BiomeRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun biomeInjection(name: String, biomeInjection: BiomeInjectionBuilder.() -> Unit) {
        buildRegistryElementLater(addon, name, Registries.BIOME, ::BiomeInjectionBuilder, biomeInjection)
    }
    
    @ExperimentalWorldGen
    fun biome(name: String, biome: BiomeBuilder.() -> Unit): ResourceKey<Biome> =
        buildRegistryElementLater(addon, name, Registries.BIOME, ::BiomeBuilder, biome)
    
}