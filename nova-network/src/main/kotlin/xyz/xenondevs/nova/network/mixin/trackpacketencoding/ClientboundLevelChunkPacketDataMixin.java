package xyz.xenondevs.nova.network.mixin.trackpacketencoding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.papermc.paper.antixray.ChunkPacketInfo;
import kotlin.Unit;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.network.PacketEncodingTrackingKt;

@Mixin(ClientboundLevelChunkPacketData.class)
abstract class ClientboundLevelChunkPacketDataMixin {
    
    @WrapOperation(
        method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lio/papermc/paper/antixray/ChunkPacketInfo;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;calculateChunkSize(Lnet/minecraft/world/level/chunk/LevelChunk;)I"
        )
    )
    private int calculateChunkSize(LevelChunk chunk, Operation<Integer> original) {
        return ScopedValue.where(PacketEncodingTrackingKt.PACKET_ENCODING, Unit.INSTANCE)
            .call(() -> original.call(chunk));
    }
    
    @WrapOperation(
        method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lio/papermc/paper/antixray/ChunkPacketInfo;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;extractChunkData(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/world/level/chunk/LevelChunk;Lio/papermc/paper/antixray/ChunkPacketInfo;)V"
        )
    )
    private void extractChunkData(
        FriendlyByteBuf buffer,
        LevelChunk chunk,
        ChunkPacketInfo<BlockState> chunkPacketInfo,
        Operation<Void> original
    ) {
        ScopedValue.where(PacketEncodingTrackingKt.PACKET_ENCODING, Unit.INSTANCE)
            .run(() -> original.call(buffer, chunk, chunkPacketInfo));
    }
    
}
