package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.SpongeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(SpongeBlock.class)
abstract class SpongeBlockMixin {
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("? instanceof BucketPickup")
    @ModifyExpressionValue(method = "removeWaterBreadthFirstSearch", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaBucketPickup(boolean original, @Local(name = "blockState") BlockState blockState) {
        return original || NovaWaterloggingBridge.isWaterloggable(blockState.getBlock());
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("(BucketPickup) ?")
    @WrapOperation(method = "removeWaterBreadthFirstSearch", at = @At("MIXINEXTRAS:EXPRESSION"))
    private BucketPickup wrapBucketPickupCast(Object block, Operation<BucketPickup> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("? instanceof BucketPickup")
    @ModifyExpressionValue(method = "lambda$removeWaterBreadthFirstSearch$1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean isNovaBucketPickupInTraversal(boolean original, @Local(name = "blockState") BlockState blockState) {
        return original || NovaWaterloggingBridge.isWaterloggable(blockState.getBlock());
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("(BucketPickup) ?")
    @WrapOperation(method = "lambda$removeWaterBreadthFirstSearch$1", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static BucketPickup wrapBucketPickupCastInTraversal(Object block, Operation<BucketPickup> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
}
