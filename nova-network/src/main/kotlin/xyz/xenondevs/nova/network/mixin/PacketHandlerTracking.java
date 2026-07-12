package xyz.xenondevs.nova.network.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.network.VanillaPacketListenerTrackingKt;

@Mixin(Connection.class)
abstract class PacketHandlerTrackingConnectionMixin {
    
    @Redirect(
        method = "genericsFtw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    private static <T extends PacketListener> void xd(Packet<T> instance, T t) {
        ScopedValue.where(VanillaPacketListenerTrackingKt.ACTIVE_PACKET_LISTENER, t)
            .run(() -> instance.handle(t));
    }
    
}

@Mixin(PacketProcessor.ListenerAndPacket.class)
abstract class PacketHandlerTrackingListenerAndPacketMixin {
    
    @Redirect(
        method = "handle", 
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V"
        )
    )
    private static <T extends PacketListener> void xd(Packet<T> instance, T t) {
        ScopedValue.where(VanillaPacketListenerTrackingKt.ACTIVE_PACKET_LISTENER, t)
            .run(() -> instance.handle(t));
    }
    
}