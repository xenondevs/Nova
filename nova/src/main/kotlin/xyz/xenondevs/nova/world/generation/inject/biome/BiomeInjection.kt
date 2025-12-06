package xyz.xenondevs.nova.world.generation.inject.biome

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import org.jetbrains.annotations.ApiStatus
import xyz.xenondevs.nova.util.data.IdentifierOrTagKey
import xyz.xenondevs.nova.util.getValueOrThrow

@ApiStatus.Internal
@JvmRecord
data class BiomeInjection(
    val biomes: Either<List<IdentifierOrTagKey<Biome>>, IdentifierOrTagKey<Biome>>,
    val features: List<HolderSet<PlacedFeature>>
) {
    
    companion object {
        
        private val BIOME_CODEC = IdentifierOrTagKey.codec(Registries.BIOME);
        val CODEC = RecordCodecBuilder.create { builder ->
            builder.group(
                Codec.either(BIOME_CODEC.listOf(), BIOME_CODEC).fieldOf("biomes").forGetter(BiomeInjection::biomes),
                PlacedFeature.LIST_OF_LISTS_CODEC.fieldOf("features").forGetter(BiomeInjection::features)
            ).apply(builder, ::BiomeInjection)
        }
        
    }
    
    fun resolveAffectedBiomes(registry: Registry<Biome>): Set<Biome> =
        biomes.map({ it }, ::listOf)
            .flatMapTo(HashSet()) { locOrTag ->
                if (locOrTag.isTag) {
                    registry.getOrThrow(locOrTag.tag).mapTo(HashSet()) { it.value() }
                } else {
                    listOf(registry.getValueOrThrow(locOrTag.location))
                }
            }
    
}