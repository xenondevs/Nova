package xyz.xenondevs.nova.world.generation.inject.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import xyz.xenondevs.nova.registry.NovaRegistries;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.generation.wrapper.WrapperBlock;

@SuppressWarnings("unused")
public final class BlockNovaMaterialDecoder {
    
    private BlockNovaMaterialDecoder() {
        throw new UnsupportedOperationException();
    }
    
    public static <T> DataResult<Pair<Holder<Block>, T>> decodeToPair(ResourceLocation location, T second) {
        var materialName = location.toString();
        var material = NMSUtilsKt.get(NovaRegistries.BLOCK, materialName);
        if (material == null)
            return DataResult.error(() -> "Unknown material: " + materialName);
        return DataResult.success(Pair.of(Holder.direct(new WrapperBlock(material)), second), Lifecycle.stable());
    }
    
    public static DataResult<Block> decodeToBlock(ResourceLocation location) {
        var materialName = location.toString();
        var material = NMSUtilsKt.get(NovaRegistries.BLOCK, materialName);
        if (material == null)
            return DataResult.error(() -> "Unknown material: " + materialName);
        return DataResult.success(new WrapperBlock(material), Lifecycle.stable());
    }
}
