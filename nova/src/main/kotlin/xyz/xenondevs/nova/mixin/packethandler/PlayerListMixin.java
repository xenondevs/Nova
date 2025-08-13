package xyz.xenondevs.nova.mixin.packethandler;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.network.PacketManager;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void placeNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        PacketManager.INSTANCE.handlePlayerCreated(player, connection);
    }
    
}
