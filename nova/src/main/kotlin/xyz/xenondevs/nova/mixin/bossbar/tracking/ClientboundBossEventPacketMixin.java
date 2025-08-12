package xyz.xenondevs.nova.mixin.bossbar.tracking;

import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayManager;

@Mixin(ClientboundBossEventPacket.class)
abstract class ClientboundBossEventPacketMixin {
    
    @Inject(
        method = "createAddPacket",
        at = @At("HEAD")
    )
    private static void handleBossBarAddPacket(BossEvent event, CallbackInfoReturnable<ClientboundBossEventPacket> cir) {
        BossBarOverlayManager.INSTANCE.handleBossBarAddPacketCreation$nova(event);
    }
    
}
