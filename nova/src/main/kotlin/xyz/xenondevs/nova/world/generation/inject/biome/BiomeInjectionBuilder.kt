package xyz.xenondevs.nova.world.generation.inject.biome

import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.data.ElementLocationOrTagKey
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@ExperimentalWorldGen
class BiomeInjectionBuilder(id: ResourceLocation): RegistryElementBuilder<BiomeInjection>(NovaRegistries.BIOME_INJECTION, id) {
    
    private val biomes = mutableListOf<ElementLocationOrTagKey<Biome>>()
    private val features = Array(11) { mutableListOf<Holder<PlacedFeature>>() }
    
    fun biome(biome: ResourceLocation): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.left(biome)))
        return this
    }
    
    fun biomes(biomeTag: TagKey<Biome>): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.right(biomeTag)))
        return this
    }
    
    fun biome(biome: String): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.left(ResourceLocation(biome))))
        return this
    }
    
    fun biomes(vararg biomes: ResourceLocation): BiomeInjectionBuilder {
        biomes.forEach(::biome)
        return this
    }
    
    fun biomes(vararg biomes: String): BiomeInjectionBuilder {
        biomes.forEach(::biome)
        return this
    }
    
    fun feature(index: GenerationStep.Decoration, feature: PlacedFeature): BiomeInjectionBuilder {
        features[index.ordinal].add(Holder.direct(feature))
        return this
    }
    
    fun feature(index: GenerationStep.Decoration, feature: Holder<PlacedFeature>): BiomeInjectionBuilder {
        features[index.ordinal].add(feature)
        return this
    }
    
    override fun build(): BiomeInjection {
        return BiomeInjection(
            if (biomes.size == 1) Either.right(biomes[0]) else Either.left(biomes),
            features.map { HolderSet.direct(it) }
        )
    }
    
}