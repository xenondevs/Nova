package xyz.xenondevs.nova.world.generation.inject.codec.blockstate;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.world.level.block.Block;
import xyz.xenondevs.nova.material.BlockNovaMaterial;
import xyz.xenondevs.nova.material.NovaMaterialRegistry;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

public final class NovaBlockCodec implements Codec<Block> {
    
    private final Codec<Block> parent;
    
    public NovaBlockCodec(Codec<Block> parent) {
        this.parent = parent;
    }
    
    @Override
    public <T> DataResult<Pair<Block, T>> decode(DynamicOps<T> ops, T input) {
        return Codec.STRING.decode(ops, input)
            .flatMap(pair -> {
                var materialName = pair.getFirst();
                if (materialName.startsWith("minecraft:"))
                    return parent.decode(ops, input);
                var material = NovaMaterialRegistry.INSTANCE.getOrNull(materialName);
                if (material == null)
                    return DataResult.error("Unknown material: " + materialName);
                if (material instanceof BlockNovaMaterial blockMaterial)
                    return DataResult.success(Pair.of(new WrapperBlock(blockMaterial), pair.getSecond()));
                return DataResult.error("Material is not a block: " + materialName);
            });
    }
    
    @Override
    public <T> DataResult<T> encode(Block input, DynamicOps<T> ops, T prefix) {
        return parent.encode(input, ops, prefix);
    }
}
