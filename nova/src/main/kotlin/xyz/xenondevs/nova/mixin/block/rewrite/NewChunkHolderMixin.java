package xyz.xenondevs.nova.mixin.block.rewrite;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.SupervisorKt;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.NovaBootstrapperKt;
import xyz.xenondevs.nova.util.AsyncExecutor;
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy;
import xyz.xenondevs.nova.world.block.tileentity.TileEntity;

import java.util.List;
import java.util.concurrent.CancellationException;

@Mixin(NewChunkHolder.class)
abstract class NewChunkHolderMixin {
    
    @Unique
    public Job nova$coroutineSupervisor = null;
    
    @Definition(id = "get", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;get()Lca/spottedleaf/moonrise/common/PlatformHooks;")
    @Definition(id = "onChunkTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Expression("get().onChunkTicking(?, ?)")
    @Inject(
        method = "handleFullStatusChange",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER)
    )
    private void startTickingNovaChunk(
        List<NewChunkHolder> changedFullStatus,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "chunk") LevelChunk chunk
    ) {
        nova$coroutineSupervisor = SupervisorKt.SupervisorJob(AsyncExecutor.SUPERVISOR);
        for (var be : chunk.blockEntities.values()) {
            if (be instanceof NovaTileEntityProxy p && p.getTileEntity() instanceof TileEntity te) {
                te.setCoroutineSupervisor$nova(SupervisorKt.SupervisorJob(nova$coroutineSupervisor));
                te.setTicking$nova(true);
                try {
                    te.handleEnableTicking();
                } catch (Throwable t) {
                    NovaBootstrapperKt.getLOGGER().error("Failed to enable ticking for {}", te, t);
                }
            }
        }
    }
    
    @Definition(id = "get", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;get()Lca/spottedleaf/moonrise/common/PlatformHooks;")
    @Definition(id = "onChunkNotTicking", method = "Lca/spottedleaf/moonrise/common/PlatformHooks;onChunkNotTicking(Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/server/level/ChunkHolder;)V")
    @Expression("get().onChunkNotTicking(?, ?)")
    @Inject(
        method = "handleFullStatusChange",
        at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER)
    )
    private void stopTickingNovaChunk(
        List<NewChunkHolder> changedFullStatus,
        CallbackInfoReturnable<Boolean> cir,
        @Local(name = "chunk") LevelChunk chunk
    ) {
        for (var be : chunk.blockEntities.values()) {
            if (be instanceof NovaTileEntityProxy p && p.getTileEntity() instanceof TileEntity te) {
                te.setCoroutineSupervisor$nova(null);
                te.setTicking$nova(false);
                try {
                    te.handleDisableTicking();
                } catch (Throwable t) {
                    NovaBootstrapperKt.getLOGGER().error("Failed to disable ticking for {}", te, t);
                }
            }
        }
        nova$coroutineSupervisor.cancel(new CancellationException("Ticking disabled for chunk"));
        nova$coroutineSupervisor = null;
    }
    
}