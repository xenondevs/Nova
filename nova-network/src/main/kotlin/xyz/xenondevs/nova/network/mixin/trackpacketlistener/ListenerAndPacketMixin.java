package xyz.xenondevs.nova.network.mixin.trackpacketlistener;

import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.network.VanillaPacketListenerTrackingKt;

@Mixin(PacketProcessor.ListenerAndPacket.class)
abstract class ListenerAndPacketMixin {
    
    @Redirect(
        method = "handle",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, T listener) {
        ScopedValue.where(VanillaPacketListenerTrackingKt.ACTIVE_PACKET_LISTENER, listener)
            .run(() -> packet.handle(listener));
    }
    
}
