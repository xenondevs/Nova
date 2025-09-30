package xyz.xenondevs.nova.mixin.packet.broadcast;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.NMSUtils;

@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    
    @Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
    private void broadcast(
        Player except,
        double x,
        double y,
        double z,
        double radius,
        ResourceKey<Level> dimension,
        Packet<?> packet,
        CallbackInfo ci
    ) {
        if (NMSUtils.broadcastDropAll.get())
            ci.cancel();
    }
    
    @ModifyVariable(method = "broadcast", at = @At("HEAD"), argsOnly = true)
    private Player modifyExcludedPlayer(Player player) {
        if (NMSUtils.broadcastIgnoreExcludedPlayer.get())
            return null;
        
        var override = NMSUtils.broadcastExcludedPlayerOverride.get();
        if (override != null)
            return override;
        
        return player;
    }
    
}
