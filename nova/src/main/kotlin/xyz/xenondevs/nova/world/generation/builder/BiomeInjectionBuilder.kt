package xyz.xenondevs.nova.world.generation.builder

import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.data.ElementLocationOrTagKey
import xyz.xenondevs.nova.util.getOrCreateHolder
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

@ExperimentalWorldGen
class BiomeInjectionBuilder(id: ResourceLocation): RegistryElementBuilder<BiomeInjection>(NovaRegistries.BIOME_INJECTION, id) {
    
    private val biomes = mutableListOf<ElementLocationOrTagKey<Biome>>()
    private val features = Array(11) { mutableListOf<Holder<PlacedFeature>>() }
    
    fun biome(biome: ResourceLocation): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.left(biome)))
        return this
    }
    
    fun biome(biome: ResourceKey<Biome>): BiomeInjectionBuilder {
        return biome(biome.location())
    }
    
    fun biome(biome: String): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.left(ResourceLocation(biome))))
        return this
    }
    
    fun biomes(biomeTag: TagKey<Biome>): BiomeInjectionBuilder {
        biomes.add(ElementLocationOrTagKey(Either.right(biomeTag)))
        return this
    }
    
    fun biomes(vararg biomes: ResourceLocation): BiomeInjectionBuilder {
        biomes.forEach(::biome)
        return this
    }
    
    fun biomes(vararg biomes: ResourceKey<Biome>): BiomeInjectionBuilder {
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
    
    fun feature(index: GenerationStep.Decoration, featureId: ResourceLocation): BiomeInjectionBuilder {
        features[index.ordinal].add(VanillaRegistries.PLACED_FEATURE.getOrCreateHolder(featureId))
        return this
    }
    
    fun feature(index: GenerationStep.Decoration, featureKey: ResourceKey<PlacedFeature>): BiomeInjectionBuilder {
        return feature(index, featureKey.location())
    }
    
    fun features(index: GenerationStep.Decoration, vararg placedFeatures: PlacedFeature): BiomeInjectionBuilder {
        features[index.ordinal].addAll(placedFeatures.map { Holder.direct(it) })
        return this
    }
    
    fun features(index: GenerationStep.Decoration, vararg placedFeatures: Holder<PlacedFeature>): BiomeInjectionBuilder {
        features[index.ordinal].addAll(placedFeatures)
        return this
    }
    
    fun features(index: GenerationStep.Decoration, vararg placedFeatureIds: ResourceLocation): BiomeInjectionBuilder {
        features[index.ordinal].addAll(placedFeatureIds.map { VanillaRegistries.PLACED_FEATURE.getOrCreateHolder(it) })
        return this
    }
    
    fun features(index: GenerationStep.Decoration, vararg placedFeatureKeys: ResourceKey<PlacedFeature>): BiomeInjectionBuilder {
        placedFeatureKeys.forEach { feature(index, it.location()) }
        return this
    }
    
    override fun build(): BiomeInjection {
        return BiomeInjection(
            if (biomes.size == 1) Either.right(biomes[0]) else Either.left(biomes),
            features.map { HolderSet.direct(it) }
        )
    }
    
}