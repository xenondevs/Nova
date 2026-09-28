package xyz.xenondevs.nova.mixin.block.sound;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.block.logic.sound.SoundEngine;

@Mixin(Entity.class)
abstract class EntityMixin {
    
    @WrapOperation(
        method = {
            "playStepSound",
            "playMuffledStepSound",
            "playCombinationStepSounds"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
        )
    )
    private void playStepSound(
        Entity entity,
        SoundEvent sound,
        float volume,
        float pitch,
        Operation<Void> original,
        @Local(argsOnly = true, ordinal = 0) BlockState state
    ) {
        if (!(entity instanceof Player player)) {
            original.call(entity, sound, volume, pitch);
            return;
        }
        
        var clientsideState = state;
        if (state.getBlock() instanceof NovaBlock block) {
            var replacement = block.getClientsideBlockStates().get(state);
            if (replacement != null)
                clientsideState = replacement;
        }
        
        SoundEngine.broadcast(
            player,
            clientsideState.getSoundType().getStepSound(),
            sound,
            volume,
            pitch
        );
    }
    
}
