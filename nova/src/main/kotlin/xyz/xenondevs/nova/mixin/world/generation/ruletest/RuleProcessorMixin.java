package xyz.xenondevs.nova.mixin.world.generation.ruletest;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.generation.ruletest.NovaRuleTest;

/**
 * Allows {@link NovaRuleTest} to be used in {@link ProcessorRule}.
 */
@Mixin(RuleProcessor.class)
abstract class RuleProcessorMixin {
    
    @Redirect(
        method = "processBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/ProcessorRule;test(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"
        )
    )
    private boolean inject(
        ProcessorRule rule,
        BlockState inputState,
        BlockState existingState,
        BlockPos localPos,
        BlockPos relativePos,
        BlockPos structurePos,
        RandomSource random,
        @Local(argsOnly = true) LevelReader level
    ) {
        if (rule.inputPredicate instanceof NovaRuleTest)
            throw new IllegalArgumentException("Input predicate of ProcessorRule must not be a NovaRuleTest"); // TODO
        
        if (!rule.inputPredicate.test(inputState, random))
            return false;
        
        if (rule.locPredicate instanceof NovaRuleTest locPredicate) {
            if (level instanceof WorldGenRegion wgr && !locPredicate.test(wgr.getLevel(), relativePos, existingState, random)) {
                return false;
            }
        } else if (!rule.locPredicate.test(existingState, random)) {
            return false;
        }
        
        return rule.posPredicate.test(localPos, relativePos, structurePos, random);
    }
    
}
