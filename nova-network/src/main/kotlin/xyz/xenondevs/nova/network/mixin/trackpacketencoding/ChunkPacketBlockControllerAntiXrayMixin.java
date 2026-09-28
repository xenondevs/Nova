package xyz.xenondevs.nova.network.mixin.trackpacketencoding;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.papermc.paper.antixray.ChunkPacketBlockControllerAntiXray;
import io.papermc.paper.antixray.ChunkPacketInfoAntiXray;
import kotlin.Unit;
import org.spongepowered.asm.mixin.Mixin;
import xyz.xenondevs.nova.network.PacketEncodingTrackingKt;

@Mixin(ChunkPacketBlockControllerAntiXray.class)
abstract class ChunkPacketBlockControllerAntiXrayMixin {
    
    @WrapMethod(method = "obfuscate")
    private void obfuscate(ChunkPacketInfoAntiXray chunkPacketInfoAntiXray, Operation<Void> original) {
        ScopedValue.where(PacketEncodingTrackingKt.PACKET_ENCODING, Unit.INSTANCE)
            .run(() -> original.call(chunkPacketInfoAntiXray));
    }
    
}
