package xyz.xenondevs.nova.world.generation.inject.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import xyz.xenondevs.nova.material.BlockNovaMaterial;
import xyz.xenondevs.nova.material.NovaMaterialRegistry;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

@SuppressWarnings("unused")
public final class BlockNovaMaterialDecoder {
    
    private BlockNovaMaterialDecoder() {
        throw new UnsupportedOperationException();
    }
    
    public static <T> DataResult<Pair<Holder<Block>, T>> decodeToPair(ResourceLocation location, T second) {
        var materialName = location.toString();
        var material = NovaMaterialRegistry.INSTANCE.getOrNull(materialName);
        if (material == null)
            return DataResult.error("Unknown material: " + materialName);
        if (material instanceof BlockNovaMaterial blockMaterial)
            return DataResult.success(Pair.of(Holder.direct(new WrapperBlock(blockMaterial)), second), Lifecycle.stable());
        return DataResult.error("Material is not a block: " + materialName);
    }
    
    public static DataResult<Block> decodeToBlock(ResourceLocation location) {
        var materialName = location.toString();
        var material = NovaMaterialRegistry.INSTANCE.getOrNull(materialName);
        if (material == null)
            return DataResult.error("Unknown material: " + materialName);
        if (material instanceof BlockNovaMaterial blockMaterial) {
            return DataResult.success(new WrapperBlock(blockMaterial), Lifecycle.stable());
        }
        return DataResult.error("Material is not a block: " + materialName);
    }
}

