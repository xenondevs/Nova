package xyz.xenondevs.nova.mixin.world.generation.ruletest;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.generation.ruletest.NovaRuleTest;

import java.util.function.Function;

/**
 * Allows {@link NovaRuleTest} to be used in {@link OreFeature}.
 */
@Mixin(OreFeature.class)
abstract class OreFeatureMixin {
    
    @Redirect(
        method = "doPlace",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/OreFeature;canPlaceOre(Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Function;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration$TargetBlockState;Lnet/minecraft/core/BlockPos$MutableBlockPos;)Z"
        )
    )
    private boolean canPlaceOre(
        BlockState state,
        Function<BlockPos, BlockState> adjacentStateAccessor,
        RandomSource random,
        OreConfiguration config,
        OreConfiguration.TargetBlockState targetState,
        BlockPos.MutableBlockPos pos,
        @Local(argsOnly = true) WorldGenLevel level
    ) {
        if (!(targetState.target instanceof NovaRuleTest ruleTest))
            return OreFeature.canPlaceOre(state, adjacentStateAccessor, random, config, targetState, pos);
        
        return ruleTest.test(level.getLevel(), pos, state, random);
    }
    
}
