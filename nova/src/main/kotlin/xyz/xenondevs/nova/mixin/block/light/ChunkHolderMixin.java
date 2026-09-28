package xyz.xenondevs.nova.mixin.block.light;

import net.minecraft.server.level.ChunkHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Patches ChunkHolder to always send light updates since we can't rely on client-side prediction
@Mixin(ChunkHolder.class)
abstract class ChunkHolderMixin {
    
    @ModifyArg(
        method = "broadcastChanges",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ChunkHolder;moonrise$getPlayers(Z)Ljava/util/List;",
            ordinal = 0
        )
    )
    private boolean sendLightUpdatesToAllTrackingPlayers(boolean onlyOnWatchDistanceEdge) {
        return false;
    }
    
}
