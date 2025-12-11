package xyz.xenondevs.nova.world.generation.builder

import com.mojang.datafixers.util.Either
import net.kyori.adventure.key.Key
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.resources.RegistryOps.RegistryInfoLookup
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.RegistryElementBuilder
import xyz.xenondevs.nova.util.data.IdentifierOrTagKey
import xyz.xenondevs.nova.util.lookupGetterOrThrow
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

/**
 * Builder for [BiomeInjections][BiomeInjection].
 * Check out the [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/inject/biome/) on biome injections for more information.
 */
@ExperimentalWorldGen
class BiomeInjectionBuilder internal constructor(
    id: Key,
    lookup: RegistryInfoLookup
) : RegistryElementBuilder<BiomeInjection>(NovaRegistries.BIOME_INJECTION, id) {
    
    private val placedFeatureRegistry = lookup.lookupGetterOrThrow(Registries.PLACED_FEATURE)
    
    private val biomes = mutableListOf<IdentifierOrTagKey<Biome>>()
    private val features = Array(GenerationStep.Decoration.entries.size) { mutableListOf<Holder<PlacedFeature>>() }
    
    /**
     * Adds [biomeTags] to the list of biomes this [BiomeInjection] should be applied to.
     */
    fun biomes(vararg biomeTags: TagKey<Biome>) {
        for (biomeTag in biomeTags) {
            biomes += IdentifierOrTagKey.ofTag(biomeTag)
        }
    }
    
    /**
     * Adds [biomes] to the list of biomes this [BiomeInjection] should be applied to.
     */
    fun biomes(vararg biomes: ResourceKey<Biome>) {
        for (biomeKey in biomes) {
            this@BiomeInjectionBuilder.biomes += IdentifierOrTagKey.ofLocation(biomeKey.identifier())
        }
    }
    
    /**
     * Adds [features] to the list of features this [BiomeInjection] should add.
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    fun features(index: GenerationStep.Decoration, vararg features: ResourceKey<PlacedFeature>) {
        for (key in features) {
            this@BiomeInjectionBuilder.features[index.ordinal] += placedFeatureRegistry.getOrThrow(key)
        }
    }
    
    /**
     * Builds a [BiomeInjection] instance from the current state of this builder.
     */
    override fun build(): BiomeInjection {
        return BiomeInjection(
            if (biomes.size == 1) Either.right(biomes[0]) else Either.left(biomes),
            features.map { HolderSet.direct(it) }
        )
    }
    
}