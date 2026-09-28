package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(BucketItem.class)
abstract class BucketItemMixin {
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("? instanceof LiquidBlockContainer")
    @ModifyExpressionValue(method = "use", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaLiquidBlockContainerInUse(boolean original, @Local(name = "clicked") BlockState clicked) {
        return original || nova$isWaterloggableNovaBlock(clicked);
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("? instanceof LiquidBlockContainer")
    @ModifyExpressionValue(
        method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Z",
        at = @At("MIXINEXTRAS:EXPRESSION"),
        require = 2,
        expect = 2
    )
    private boolean isNovaLiquidBlockContainerInEmptyContents(boolean original, @Local(name = "blockState") BlockState blockState) {
        return original || nova$isWaterloggableNovaBlock(blockState);
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("(LiquidBlockContainer) ?")
    @WrapOperation(
        method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Z",
        at = @At("MIXINEXTRAS:EXPRESSION"),
        require = 2,
        expect = 2
    )
    private LiquidBlockContainer wrapLiquidBlockContainerCast(Object block, Operation<LiquidBlockContainer> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("? instanceof BucketPickup")
    @ModifyExpressionValue(method = "use", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaBucketPickup(boolean original, @Local(name = "blockState") BlockState blockState) {
        return original || nova$isWaterloggableNovaBlock(blockState);
    }
    
    @Definition(id = "BucketPickup", type = BucketPickup.class)
    @Expression("(BucketPickup) ?")
    @WrapOperation(method = "use", at = @At("MIXINEXTRAS:EXPRESSION"))
    private BucketPickup wrapBucketPickupCast(Object block, Operation<BucketPickup> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
    @Unique
    private static boolean nova$isWaterloggableNovaBlock(BlockState state) {
        return NovaWaterloggingBridge.isWaterloggable(state.getBlock());
    }
    
}
