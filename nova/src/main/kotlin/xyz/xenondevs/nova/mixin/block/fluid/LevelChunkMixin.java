package xyz.xenondevs.nova.mixin.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
    
    @Shadow
    @Final
    private ServerLevel level;
    
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void scheduleNovaWaterTick(
        BlockPos pos,
        BlockState state,
        int flags,
        CallbackInfoReturnable<BlockState> cir
    ) {
        BlockState oldState = cir.getReturnValue();
        if (oldState == null || !(state.getBlock() instanceof NovaBlock))
            return;
        if (oldState.getBlock() == state.getBlock()
            && oldState.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
            return;
        }
        
        NovaWaterloggingBridge.scheduleWaterTickIfWaterlogged(state, level, pos);
    }
    
}
