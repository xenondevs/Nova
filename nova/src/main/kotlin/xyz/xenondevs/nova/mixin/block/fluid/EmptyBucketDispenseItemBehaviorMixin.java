package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.BucketPickup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$4")
@SuppressWarnings("UnresolvedMixinReference")
abstract class EmptyBucketDispenseItemBehaviorMixin {
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("? instanceof BucketPickup")
    @WrapOperation(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaBucketPickup(Object block, Operation<Boolean> original) {
        return original.call(block) || NovaWaterloggingBridge.isWaterloggable(block);
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("(BucketPickup) ?")
    @WrapOperation(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"))
    private BucketPickup wrapBucketPickupCast(Object block, Operation<BucketPickup> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
}
