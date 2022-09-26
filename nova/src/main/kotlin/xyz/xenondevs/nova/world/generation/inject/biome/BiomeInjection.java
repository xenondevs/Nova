package xyz.xenondevs.nova.world.generation.inject.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

public record BiomeInjection(TagKey<Biome> biomesTag, List<HolderSet<PlacedFeature>> features) {
    
    public static final Codec<BiomeInjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        TagKey.codec(Registry.BIOME_REGISTRY).fieldOf("biomes").forGetter(BiomeInjection::biomesTag),
        PlacedFeature.LIST_OF_LISTS_CODEC.fieldOf("features").forGetter(BiomeInjection::features)
    ).apply(instance, BiomeInjection::new));
    
}
