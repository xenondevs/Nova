package xyz.xenondevs.nova.mixin.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.NovaBootstrapperKt;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider;
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider;
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider;
import xyz.xenondevs.nova.world.format.WorldDataManager;

@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockBehaviourBlockStateBaseMixin {
    
    @Inject(method = "handleNeighborChanged", at = @At("HEAD"), cancellable = true)
    private void handleNeighborChanged(
        Level level,
        BlockPos pos,
        Block neighborBlock,
        Orientation orientation,
        boolean movedByPiston,
        CallbackInfo ci
    ) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return;
        
        try {
            novaState.getBlock().handleNeighborChanged(novaPos, novaState);
        } catch (Exception e) {
            NovaBootstrapperKt.getLOGGER().error("Failed to handle neighbor change for {} at {}", novaState, novaPos, e);
        }
        ci.cancel();
    }
    
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void updateShape(
        LevelReader level,
        ScheduledTickAccess scheduledTickAccess,
        BlockPos pos,
        Direction direction,
        BlockPos neighborPos,
        BlockState neighborState,
        RandomSource random,
        CallbackInfoReturnable<BlockState> cir
    ) {
        // fixme: needs to support WorldGenRegion
        if (!(level instanceof ServerLevel serverLevel))
            return;
        
        var novaPos = NMSUtilsKt.toNovaPos(pos, serverLevel.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return;
        
        try {
            var novaNeighborPos = NMSUtilsKt.toNovaPos(neighborPos, serverLevel.getWorld());
            var newState = novaState.getBlock().updateShape(novaPos, novaState, novaNeighborPos);
            if (newState == novaState) {
                cir.setReturnValue((BlockState) (Object) this);
                return;
            }
            
            WorldDataManager.INSTANCE.setBlockState(novaPos, newState);
            var ret = switch (newState.getModelProvider$nova()) {
                case BackingStateBlockModelProvider modelProvider -> modelProvider.getInfo().getVanillaBlockState();
                case ModelLessBlockModelProvider modelProvider -> modelProvider.getInfo();
                case DisplayEntityBlockModelProvider modelProvider -> {
                    novaState.getModelProvider$nova().unload(novaPos);
                    newState.getModelProvider$nova().load(novaPos);
                    yield modelProvider.getInfo().getHitboxType();
                }
                default -> throw new UnsupportedOperationException();
            };
            cir.setReturnValue(ret);
        } catch (Exception e) {
            NovaBootstrapperKt.getLOGGER().error("Failed to update shape for {} at {}", novaState, novaPos, e);
        }
        cir.cancel();
    }
    
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return;
        
        try {
            novaState.getBlock().handleScheduledTick(novaPos, novaState);
        } catch (Exception e) {
            NovaBootstrapperKt.getLOGGER().error("Failed to handle vanilla scheduled tick for {} at {}", novaState, novaPos, e);
        }
        ci.cancel();
    }
    
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void entityInside(
        Level level,
        BlockPos pos,
        Entity entity,
        InsideBlockEffectApplier effectApplier,
        boolean flag,
        CallbackInfo ci
    ) {
        var novaPos = NMSUtilsKt.toNovaPos(pos, level.getWorld());
        var novaState = WorldDataManager.INSTANCE.getBlockState(novaPos);
        if (novaState == null)
            return;
        
        try {
            novaState.getBlock().handleEntityInside(novaPos, novaState, entity.getBukkitEntity());
        } catch (Exception e) {
            NovaBootstrapperKt.getLOGGER().error("Failed to handle entity inside for {} at {}", novaState, novaPos, e);
        }
        ci.cancel();
    }
    
}
