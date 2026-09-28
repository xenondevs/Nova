package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.LiquidBlockContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(targets = "net.minecraft.core.dispenser.DispenseItemBehavior$3")
@SuppressWarnings("UnresolvedMixinReference")
abstract class FilledBucketDispenseItemBehaviorMixin {
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("? instanceof LiquidBlockContainer")
    @WrapOperation(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isNovaLiquidBlockContainer(Object block, Operation<Boolean> original) {
        return original.call(block) || NovaWaterloggingBridge.isWaterloggable(block);
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("(LiquidBlockContainer) ?")
    @WrapOperation(method = "execute", at = @At("MIXINEXTRAS:EXPRESSION"))
    private LiquidBlockContainer wrapLiquidBlockContainerCast(Object block, Operation<LiquidBlockContainer> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
}
