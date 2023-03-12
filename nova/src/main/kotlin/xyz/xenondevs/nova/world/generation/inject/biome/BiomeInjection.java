package xyz.xenondevs.nova.world.generation.inject.biome;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import xyz.xenondevs.nova.util.NMSUtils;
import xyz.xenondevs.nova.util.data.DataFixerUpperUtils;
import xyz.xenondevs.nova.util.data.ElementLocationOrTagKey;
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;


@ExperimentalWorldGen
public record BiomeInjection(
    Either<List<ElementLocationOrTagKey<Biome>>, ElementLocationOrTagKey<Biome>> biomes,
    List<HolderSet<PlacedFeature>> features
) {
    
    private static final Registry<Biome> BIOME_REGISTRY = NMSUtils.INSTANCE.getRegistry(Registries.BIOME);
    private static final Codec<ElementLocationOrTagKey<Biome>> BIOME_CODEC = DataFixerUpperUtils.tagOrElementCodec(Registries.BIOME);
    
    public static final Codec<BiomeInjection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.either(BIOME_CODEC.listOf(), BIOME_CODEC).fieldOf("biomes").forGetter(BiomeInjection::biomes),
        PlacedFeature.LIST_OF_LISTS_CODEC.fieldOf("features").forGetter(BiomeInjection::features)
    ).apply(instance, BiomeInjection::new));
    
    public Set<ResourceLocation> getBiomes() {
        var list = biomes.map(Function.identity(), List::of);
        var out = new ObjectOpenHashSet<ResourceLocation>();
        for (var tagOrElement : list) {
            if (tagOrElement.isTag()) {
                var tagKey = tagOrElement.getTag();
                var biomes = BIOME_REGISTRY.getTag(tagKey);
                if (biomes.isEmpty())
                    throw new IllegalStateException("Biome tag " + tagKey + " does not exist!");
                biomes.get().stream().forEach(holder -> out.add(BIOME_REGISTRY.getKey(holder.value())));
            } else if (tagOrElement.isElement()) {
                var resourceLocation = tagOrElement.getElement();
                if (!BIOME_REGISTRY.containsKey(resourceLocation))
                    throw new IllegalArgumentException("Biome " + resourceLocation + " does not exist!");
                out.add(resourceLocation);
            } else throw new IllegalStateException("BiomeInjection has neither a tag nor a resource location!");
        }
        return out;
    }
    
    public static BiomeInjectionBuilder builder() {
        return new BiomeInjectionBuilder();
    }
    
    public static BiomeInjection builder(Consumer<BiomeInjectionBuilder> builder) {
        var biomeInjectionBuilder = new BiomeInjectionBuilder();
        builder.accept(biomeInjectionBuilder);
        return biomeInjectionBuilder.build();
    }
    
}
