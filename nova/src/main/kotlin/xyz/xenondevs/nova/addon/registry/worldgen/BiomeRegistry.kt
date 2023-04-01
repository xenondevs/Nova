package xyz.xenondevs.nova.addon.registry.worldgen

import net.minecraft.world.level.biome.Biome
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.builder.BiomeBuilder
import xyz.xenondevs.nova.world.generation.builder.BiomeInjectionBuilder
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

interface BiomeRegistry : AddonGetter {
    
    @ExperimentalWorldGen
    fun biomeInjection(name: String) = BiomeInjectionBuilder(ResourceLocation(addon, name))
    
    @ExperimentalWorldGen
    fun biome(name: String) = BiomeBuilder(ResourceLocation(addon, name))
    
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