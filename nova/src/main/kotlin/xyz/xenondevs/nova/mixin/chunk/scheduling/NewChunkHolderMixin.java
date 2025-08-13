package xyz.xenondevs.nova.mixin.chunk.scheduling;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.ChunkPos;
import xyz.xenondevs.nova.world.format.WorldDataManager;

import java.util.List;

@SuppressWarnings("LocalMayBeArgsOnly") // not true?
@Mixin(NewChunkHolder.class)
abstract class NewChunkHolderMixin {
    
    @Definition(id = "get", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;get()Lca/spottedleaf/moonrise/common/PlatformHooks;")
    @Definition(id = "onChunkTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Definition(id = "onChunkEntityTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkEntityTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Expression(value = {"get().onChunkTicking(?, ?)", "get().onChunkEntityTicking(?, ?)"})
    @Inject(
        method = "handleFullStatusChange",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER)
    )
    private void startTickingNovaChunk(
        List<NewChunkHolder> changedFullStatus,
        CallbackInfoReturnable<Boolean> cir,
        @Local LevelChunk chunk
    ) {
        var pos = new ChunkPos(chunk.level.uuid, chunk.locX, chunk.locZ);
        WorldDataManager.INSTANCE.startTicking$nova(pos);
    }
    
    @Definition(id = "get", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;get()Lca/spottedleaf/moonrise/common/PlatformHooks;")
    @Definition(id = "onChunkNotEntityTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkNotEntityTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Definition(id = "onChunkNotTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkNotTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Expression(value = {"get().onChunkNotTicking(?, ?)", "get().onChunkNotEntityTicking(?, ?)"})
    @Inject(
        method = "handleFullStatusChange",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER)
    )
    private void stopTickingNovaChunk(
        List<NewChunkHolder> changedFullStatus,
        CallbackInfoReturnable<Boolean> cir,
        @Local LevelChunk chunk
    ) {
        var pos = new ChunkPos(chunk.level.uuid, chunk.locX, chunk.locZ);
        WorldDataManager.INSTANCE.stopTicking$nova(pos);
    }
    
}
