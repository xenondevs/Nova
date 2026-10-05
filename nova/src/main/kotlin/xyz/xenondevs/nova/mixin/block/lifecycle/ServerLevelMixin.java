package xyz.xenondevs.nova.mixin.block.lifecycle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    
    @Shadow
    public abstract LevelChunk moonrise$getFullChunkIfLoaded(int chunkX, int chunkZ);
    
    @Inject(method = "onBlockEntityAdded", at = @At("HEAD"))
    private void handleNovaTileEntityEnable(BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof NovaTileEntityProxy proxy))
            return;
        
        var pos = blockEntity.getBlockPos();
        var chunk = moonrise$getFullChunkIfLoaded(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) // chunk loading enables tile entities via ChunkLoadEvent in NovaTileEntityProxy
            return;
        
        proxy.enable(chunk);
    }
    
}
