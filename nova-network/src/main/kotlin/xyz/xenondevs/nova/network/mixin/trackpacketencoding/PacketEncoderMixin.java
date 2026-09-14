package xyz.xenondevs.nova.network.mixin.trackpacketencoding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import kotlin.Unit;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.codec.StreamCodec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.network.PacketEncodingTrackingKt;

@Mixin(PacketEncoder.class)
abstract class PacketEncoderMixin {
    
    @WrapOperation(
        method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;Lio/netty/buffer/ByteBuf;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/codec/StreamCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    )
    private void encodePacket(
        StreamCodec<Object, Object> instance,
        Object buffer,
        Object packet,
        Operation<Void> original
    ) {
        ScopedValue.where(PacketEncodingTrackingKt.PACKET_ENCODING, Unit.INSTANCE)
            .run(() -> original.call(instance, buffer, packet));
    }
    
}
