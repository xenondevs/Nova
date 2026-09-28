package xyz.xenondevs.nova.mixin.block.lifecycle;


import kotlinx.coroutines.SupervisorKt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.NovaBootstrapperKt;
import xyz.xenondevs.nova.world.block.NovaTileEntityBlockKt;
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy;
import xyz.xenondevs.nova.world.block.tileentity.TileEntity;

@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    
    @Shadow
    public abstract LevelChunk moonrise$getFullChunkIfLoaded(int chunkX, int chunkZ);
    
    @Inject(method = "onBlockEntityAdded", at = @At("HEAD"))
    private void handleNovaTileEntityEnable(BlockEntity blockEntity, CallbackInfo ci) {
        if (!(blockEntity instanceof NovaTileEntityProxy p) || !(p.getTileEntity() instanceof TileEntity te))
            return;
        
        te.setEnabled$nova(true);
        try {
            te.handleEnable();
        } catch (Throwable t) {
            NovaBootstrapperKt.getLOGGER().error("Failed to enable {}", te, t);
        }
        
        var pos = blockEntity.getBlockPos();
        var chunk = moonrise$getFullChunkIfLoaded(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null)
            return;
        
        var supervisor = NovaTileEntityBlockKt.getCoroutineSupervisor(chunk.moonrise$getChunkHolder());
        if (supervisor == null) // chunk is ticking
            return;
        
        te.setCoroutineSupervisor$nova(SupervisorKt.SupervisorJob(supervisor));
        te.setTicking$nova(true);
        try {
            te.handleEnableTicking();
        } catch (Throwable t) {
            NovaBootstrapperKt.getLOGGER().error("Failed to enable ticking for {}", te, t);
        }
    }
    
}
