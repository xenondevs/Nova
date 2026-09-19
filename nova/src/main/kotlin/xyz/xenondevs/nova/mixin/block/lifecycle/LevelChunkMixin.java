package xyz.xenondevs.nova.mixin.block.lifecycle;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock;
import xyz.xenondevs.nova.world.block.NovaTileEntityProxy;
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntityManager;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    
    @Shadow
    public abstract @Nullable BlockEntity getBlockEntity(BlockPos pos);
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void captureBlockEntity(
        BlockPos pos,
        BlockState state,
        int flags,
        CallbackInfoReturnable<BlockState> cir,
        @Share("oldBlockEntity") LocalRef<BlockEntity> oldBlockEntity
    ) {
        oldBlockEntity.set(getBlockEntity(pos));
    }
    
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void handleNovaBlockBreakAndPlace(
        CallbackInfoReturnable<BlockState> cir,
        @Local(argsOnly = true, name = "pos") BlockPos pos,
        @Local(argsOnly = true, name = "state") BlockState state,
        @Share("oldBlockEntity") LocalRef<BlockEntity> oldBlockEntity
    ) {
        BlockState oldState = cir.getReturnValue();
        if (oldState == null || state == null)
            return;
        
        var oldBlock = oldState.getBlock();
        var newBlock = state.getBlock();
        var previousBlockEntity = oldBlockEntity.get();
        
        VanillaTileEntityManager.handleBlockStateChange(
            oldState,
            state,
            previousBlockEntity,
            getBlockEntity(pos)
        );
        
        if (oldBlock == newBlock) {
            if (oldState != state && oldBlock instanceof NovaBlock novaBlock) {
                novaBlock.nmsHandleStateChange(oldState, state, level, pos);
            }
            return;
        }
        
        if (oldBlock instanceof NovaTileEntityBlock oldNovaBlock) {
            oldNovaBlock.nmsHandleBreak(oldState, level, pos, state, (NovaTileEntityProxy) previousBlockEntity);
        } else if (oldBlock instanceof NovaBlock oldNovaBlock) {
            oldNovaBlock.nmsHandleBreak(oldState, level, pos, state);
        }
        
        if (newBlock instanceof NovaTileEntityBlock newNovaBlock) {
            var newNovaTileEntity = (NovaTileEntityProxy) getBlockEntity(pos);
            newNovaBlock.nmsHandlePlace(state, level, pos, oldState, newNovaTileEntity);
        } else if (newBlock instanceof NovaBlock newNovaBlock) {
            newNovaBlock.nmsHandlePlace(state, level, pos, oldState);
        }
        
    }
    
}
