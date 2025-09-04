package xyz.xenondevs.nova.mixin.item.using;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    
    /**
     * Effectively removes the line:
     * ```java
     * this.player.stopUsingItem(); // CraftBukkit - SPIGOT-4706
     * ```
     * SPIGOT-4706 does not exist, so I don't know why this is there.
     * However, it breaks server-side control of item using when right-clicking on a block,
     * because the client will send a secondary packet for the off-hand, which will cause
     * using to be stopped again because of that line.
     */
    @Redirect(
        method = "handleUseItemOn",
        at = @At(
            value = "INVOKE", 
            target="Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V"
        ),
        require = 1,
        allow = 1
    )
    private void noop(ServerPlayer instance) {}
    
}
