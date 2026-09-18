package xyz.xenondevs.nova.registry

import com.mojang.datafixers.util.Either
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.GenerationStep
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.util.data.IdentifierOrTagKey
import xyz.xenondevs.nova.world.generation.inject.biome.BiomeInjection

/**
 * Builder for [BiomeInjections][BiomeInjection].
 * Check out the [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/inject/biome/) on biome injections for more information.
 */
@RegistryElementBuilderDsl
class BiomeInjectionBuilder internal constructor() {
    
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
            this.biomes += IdentifierOrTagKey.ofLocation(biomeKey.identifier())
        }
    }
    
    /**
     * Adds [features] to the list of features this [BiomeInjection] should add.
     *
     * For more information on features, check out their [docs page](https://xenondevs.xyz/docs/nova/addon/worldgen/features/features/).
     */
    context(ctx: RegistryLookupContext)
    fun features(index: GenerationStep.Decoration, vararg features: ResourceKey<PlacedFeature>) {
        for (key in features) {
            this.features[index.ordinal] += key.holder()
        }
    }
    
    /**
     * Builds a [BiomeInjection] instance from the current state of this builder.
     */
    internal fun build(): BiomeInjection {
        return BiomeInjection(
            if (biomes.size == 1) Either.right(biomes[0]) else Either.left(biomes),
            features.map { HolderSet.direct(it) }
        )
    }
    
}