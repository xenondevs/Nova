package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.world.level.biome.Biome
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

interface BiomeRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun biomeInjection(name: String, biomeInjection: BiomeInjectionBuilder.() -> Unit): BiomeInjection =
        BiomeInjectionBuilder(ResourceLocation(addon, name)).apply(biomeInjection).register()
    
    @ExperimentalWorldGen
    fun biome(name: String, biome: BiomeBuilder.() -> Unit): Biome =
        BiomeBuilder(ResourceLocation(addon, name)).apply(biome).register()
    
    @ExperimentalWorldGen
    fun registerBiomeInjection(name: String, injection: BiomeInjection): BiomeInjection {
        val id = ResourceLocation(addon, name)
        NovaRegistries.BIOME_INJECTION[id] = injection
        return injection
    }
    
    @ExperimentalWorldGen
    fun registerBiome(name: String, biome: Biome): Biome {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.BIOME[id] = biome
        return biome
    }
    
}