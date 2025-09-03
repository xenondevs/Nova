package xyz.xenondevs.nova.mixin.world.generation.ruletest;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.generation.ruletest.NovaRuleTest;

/**
 * Allows {@link NovaRuleTest} to be used in {@link ReplaceBlockFeature}.
 */
@Mixin(ReplaceBlockFeature.class)
abstract class ReplaceBlockFeatureMixin {
    
    @Redirect(
        method = "place",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/RuleTest;test(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z"
        )
    )
    private boolean test(
        RuleTest ruleTest,
        BlockState state,
        RandomSource randomSource,
        @Local WorldGenLevel level,
        @Local BlockPos pos
    ) {
        if (!(ruleTest instanceof NovaRuleTest novaRuleTest))
            return ruleTest.test(state, randomSource);
        
        return novaRuleTest.test(level.getLevel(), pos, state, randomSource);
    }
    
}
