package xyz.xenondevs.nova.mixin.block.rewrite;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.papermc.paper.antixray.ChunkPacketInfo;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import xyz.xenondevs.nova.world.block.logic.PacketBlocks;

import static xyz.xenondevs.nova.network.PacketEncodingTrackingKt.isInPacketEncoding;

@Mixin(PalettedContainer.Data.class)
abstract class PalettedContainerDataMixin<T> {
    
    @Final
    @Shadow
    private BitStorage storage;
    
    @Final
    @Shadow
    private Palette<T> palette;
    
    @WrapMethod(method = "getSerializedSize")
    private int getClientSideSerializedSize(
        IdMap<T> globalMap,
        Operation<Integer> original
    ) {
        if (nova$isClientSideGlobalBlockStatePalette(globalMap)) {
            int bits = PacketBlocks.getClientSideBlockStateBits();
            int valuesPerLong = 64 / bits;
            int longs = (storage.getSize() + valuesPerLong - 1) / valuesPerLong;
            return 1 + longs * Long.BYTES;
        }
        
        return original.call(globalMap);
    }
    
    @WrapMethod(method = "write")
    private void writeClientSideGlobalBlockStateIds(
        FriendlyByteBuf buffer,
        IdMap<T> globalMap,
        ChunkPacketInfo<T> chunkPacketInfo,
        int chunkSectionIndex,
        Operation<Void> original
    ) {
        if (nova$isClientSideGlobalBlockStatePalette(globalMap)) {
            int bits = PacketBlocks.getClientSideBlockStateBits();
            SimpleBitStorage translated = new SimpleBitStorage(bits, storage.getSize());
            for (int i = 0; i < storage.getSize(); i++) {
                int serverId = storage.get(i);
                var value = globalMap.byId(serverId);
                if (!(value instanceof BlockState state))
                    throw new IllegalStateException("Unknown block state id in global palette: " + serverId);
                
                int clientId = PacketBlocks.getClientSideStateId(state);
                if (clientId < 0 || (long) clientId >= 1L << bits)
                    throw new IllegalStateException("Client-side block state id does not fit global palette: " + clientId);
                translated.set(i, clientId);
            }
            
            buffer.writeByte(bits);
            palette.write(buffer, globalMap);
            if (chunkPacketInfo != null) {
                chunkPacketInfo.setBits(chunkSectionIndex, bits);
                chunkPacketInfo.setPalette(chunkSectionIndex, palette);
                chunkPacketInfo.setIndex(chunkSectionIndex, buffer.writerIndex());
            }
            buffer.writeFixedSizeLongArray(translated.getRaw());
        } else {
            original.call(buffer, globalMap, chunkPacketInfo, chunkSectionIndex);
        }
    }
    
    @Unique
    private boolean nova$isClientSideGlobalBlockStatePalette(IdMap<T> globalMap) {
        return isInPacketEncoding()
               && globalMap == Block.BLOCK_STATE_REGISTRY
               && palette instanceof GlobalPalette<?>;
    }
    
}
