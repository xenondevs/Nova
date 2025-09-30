package xyz.xenondevs.nova.mixin.block.fluidflow;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.block.DefaultBlocks;
import xyz.xenondevs.nova.world.block.behavior.Waterloggable;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(FlowingFluid.class)
abstract class FlowingFluidMixin {
    
    @Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
    private static void canHoldSpecificFluid(
        BlockGetter getter,
        BlockPos pos,
        BlockState state,
        Fluid fluid,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(getter instanceof Level level))
            return;
        
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return;
        
        var novaBlock = novaState.getBlock();
        if (!novaBlock.hasBehavior(Waterloggable.class) && novaBlock != DefaultBlocks.INSTANCE.getTRIPWIRE())
            cir.setReturnValue(false);
    }
    
}
