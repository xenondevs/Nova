package xyz.xenondevs.nova.mixin.fakeplayer;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.FakePlayer;

/**
 * Prevents fake players from being stored in the lastHurtByPlayer field.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    
    @Inject(
        method = "setLastHurtByPlayer(Lnet/minecraft/world/entity/player/Player;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void setLastHurtByPlayer(Player player, int memoryTime, CallbackInfo ci) {
        if (player instanceof FakePlayer) {
            // required for experience orbs to be spawned
            ((LivingEntity) (Object) this).lastHurtByPlayerMemoryTime = memoryTime;
            
            // prevent fake player from being stored in lastHurtByPlayer field
            ci.cancel();
        }
    }
    
}
