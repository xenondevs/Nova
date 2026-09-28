package xyz.xenondevs.nova.mixin.block.fluid;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaWaterloggingBridge;

@Mixin(StructureTemplate.class)
abstract class StructureTemplateMixin {
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("? instanceof LiquidBlockContainer")
    @ModifyExpressionValue(
        method = "placeInWorld",
        at = @At("MIXINEXTRAS:EXPRESSION"),
        require = 2,
        expect = 2
    )
    private boolean isNovaLiquidBlockContainer(boolean original, @Local(name = "state") BlockState state) {
        return original || NovaWaterloggingBridge.isWaterloggable(state.getBlock());
    }
    
    @Definition(id = "LiquidBlockContainer", type = LiquidBlockContainer.class)
    @Expression("(LiquidBlockContainer) ?")
    @WrapOperation(
        method = "placeInWorld",
        at = @At("MIXINEXTRAS:EXPRESSION"),
        require = 2,
        expect = 2
    )
    private LiquidBlockContainer wrapLiquidBlockContainerCast(Object block, Operation<LiquidBlockContainer> original) {
        return NovaWaterloggingBridge.isWaterloggable(block)
            ? NovaWaterloggingBridge.INSTANCE
            : original.call(block);
    }
    
}
