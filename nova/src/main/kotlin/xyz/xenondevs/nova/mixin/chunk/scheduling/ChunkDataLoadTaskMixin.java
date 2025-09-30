package xyz.xenondevs.nova.mixin.chunk.scheduling;

import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.ChunkPos;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(ChunkLoadTask.ChunkDataLoadTask.class)
abstract class ChunkDataLoadTaskMixin {
    
    @Inject(method = "runOffMain", at = @At("HEAD"))
    private void loadNovaChunk(
        CompoundTag data,
        Throwable throwable,
        CallbackInfoReturnable<GenericDataLoadTask.TaskResult<ChunkLoadTask.ReadChunk, Throwable>> cir
    ) {
        var thisRef = (ChunkLoadTask.ChunkDataLoadTask) (Object) this;
        var chunkPos = new ChunkPos(thisRef.world.uuid, thisRef.chunkX, thisRef.chunkZ);
        WorldDataManager.INSTANCE.getOrLoadChunkBlocking$nova(chunkPos);
    }
    
}
