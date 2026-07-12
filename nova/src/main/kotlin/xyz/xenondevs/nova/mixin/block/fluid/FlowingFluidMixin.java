package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.FluidFlowMode;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(FlowingFluid.class)
abstract class FlowingFluidMixin {
    
    @Inject(method = "canHoldAnyFluid", at = @At("HEAD"), cancellable = true)
    private static void handleNovaFluidEligibility(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof NovaBlock novaBlock) {
            cir.setReturnValue(novaBlock.getFluidFlowModes().get(state).getAllowsIncomingFlow());
        }
    }
    
    @Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
    private static void handleNovaFluidEligibility(
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        Fluid newFluid,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(state.getBlock() instanceof NovaBlock novaBlock))
            return;
        
        FluidFlowMode mode = novaBlock.getFluidFlowModes().get(state);
        
        cir.setReturnValue(
            mode.getAllowsIncomingFlow()
            && (!mode.getWaterlogsIncomingFlow()
                || NovaWaterloggingBridge.INSTANCE.canPlaceLiquid(null, level, pos, state, newFluid))
        );
    }
    
    @WrapOperation(
        method = {"canMaybePassThrough", "isWaterHole"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FlowingFluid;canPassThroughWall(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        ),
        require = 2
    )
    private boolean handleNovaForwardFluidOcclusion(
        Direction direction,
        BlockGetter level,
        BlockPos fromPos,
        BlockState fromState,
        BlockPos toPos,
        BlockState toState,
        Operation<Boolean> original
    ) {
        return nova$canPassThroughWall(direction, level, fromPos, fromState, toPos, toState, true, original);
    }
    
    @WrapOperation(
        method = "getNewLiquid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/material/FlowingFluid;canPassThroughWall(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
        ),
        require = 2
    )
    private boolean handleNovaReverseFluidOcclusion(
        Direction direction,
        BlockGetter level,
        BlockPos fromPos,
        BlockState fromState,
        BlockPos toPos,
        BlockState toState,
        Operation<Boolean> original
    ) {
        return nova$canPassThroughWall(direction, level, fromPos, fromState, toPos, toState, false, original);
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("? instanceof LiquidBlockContainer")
    @ModifyExpressionValue(method = "spreadTo", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaLiquidBlockContainer(boolean original, @Local(argsOnly = true, name = "state") BlockState state) {
        if (!(state.getBlock() instanceof NovaBlock novaBlock))
            return original;
        return original || NovaWaterloggingBridge.isWaterloggable(novaBlock)
                           && novaBlock.getFluidFlowModes().get(state).getWaterlogsIncomingFlow();
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("(LiquidBlockContainer) ?")
    @WrapOperation(method = "spreadTo", at = @At("MIXINEXTRAS:EXPRESSION"))
    private LiquidBlockContainer wrapLiquidBlockContainerCast(Object block, Operation<LiquidBlockContainer> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void handleNovaFluidFlow(
        LevelAccessor level,
        BlockPos pos,
        BlockState state,
        Direction direction,
        FluidState target,
        CallbackInfo ci
    ) {
        if (state.getBlock() instanceof NovaBlock novaBlock
            && !novaBlock.getFluidFlowModes().get(state).getAllowsIncomingFlow()) {
            ci.cancel();
        }
    }
    
    @Unique
    private static boolean nova$canPassThroughWall(
        Direction direction,
        BlockGetter level,
        BlockPos fromPos,
        BlockState fromState,
        BlockPos toPos,
        BlockState toState,
        boolean fromIsSource,
        Operation<Boolean> original
    ) {
        if (!(fromState.getBlock() instanceof NovaBlock) && !(toState.getBlock() instanceof NovaBlock))
            return original.call(direction, level, fromPos, fromState, toPos, toState);
        
        VoxelShape fromShape = nova$getFluidOcclusionShape(level, fromPos, fromState, fromIsSource);
        VoxelShape toShape = nova$getFluidOcclusionShape(level, toPos, toState, !fromIsSource);
        return !Shapes.mergedFaceOccludes(fromShape, toShape, direction);
    }
    
    @Unique
    private static VoxelShape nova$getFluidOcclusionShape(
        BlockGetter level,
        BlockPos pos,
        BlockState state,
        boolean source
    ) {
        if (!(state.getBlock() instanceof NovaBlock novaBlock))
            return state.getCollisionShape(level, pos);
        
        FluidFlowMode mode = novaBlock.getFluidFlowModes().get(state);
        boolean allowsFlow = source ? mode.getAllowsOutgoingFlow() : mode.getAllowsIncomingFlow();
        return allowsFlow ? Shapes.empty() : Shapes.block();
    }
    
}
