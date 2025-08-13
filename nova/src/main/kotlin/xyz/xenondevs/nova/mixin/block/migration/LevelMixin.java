package xyz.xenondevs.nova.mixin.block.migration;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
abstract class LevelMixin {
    
    /**
     * Inserts {@code newBlock = actualBlock}.
     * I don't know why this the newBlock == actualBlock check exists, but this
     * patch is necessary to prevent desync caused by block migration. Let's hope this doesn't blow up.
     */
    @ModifyVariable(
        method = "notifyAndUpdatePhysics",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 1
    )
    private BlockState notifyAndUpdatePhysics(
        BlockState newState, 
        @Local(ordinal = 2, argsOnly = true) BlockState currentState
    ) {
       return currentState; 
    }
    
}
