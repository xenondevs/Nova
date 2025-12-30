package xyz.xenondevs.nova.mixin.item.use;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.MixinContexts;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(Player.class)
abstract class PlayerMixin {
    
    // this is necessary to prevent air being placed just because the old item stack is air now (but hand item was replaced with another item stack)
    @Inject(
        method = "interactOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;interactLivingEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            shift = At.Shift.AFTER
        )
    )
    private void updateItemInHandVariable(
        Entity entityToInteractOn,
        InteractionHand hand,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local(name = "itemInHand") LocalRef<ItemStack> itemInHand
    ) {
        var player = (Player) (Object) this;
        itemInHand.set(player.getItemInHand(hand));
    }
    
    // lets nova handle non-living-enitity interactions, other interactions are handled in ItemStackMixin
    @Definition(id = "PASS", field = "Lnet/minecraft/world/InteractionResult;PASS:Lnet/minecraft/world/InteractionResult$Pass;")
    @Expression("return PASS")
    @ModifyReturnValue(
        method = "interactOn",
        at = @At(value = "MIXINEXTRAS:EXPRESSION")
    )
    private InteractionResult useNovaItemOnNonLivingEntity(
        InteractionResult original,
        @Local(argsOnly = true) Entity entity,
        @Local(argsOnly = true) InteractionHand hand
    ) {
        var player = (Player) (Object) this;
        if (player.isSpectator() || entity instanceof LivingEntity)
            return original;
        var itemInHand = player.getItemInHand(hand);
        var novaItem = ItemUtilsKt.getNovaItem(itemInHand);
        if (novaItem == null)
            return original;
        
        var result = novaItem.useOnEntityNms$nova(player, itemInHand, entity, hand, MixinContexts.INTERACT_LOCATION.get());
        if (result instanceof InteractionResult.Pass)
            result = novaItem.useNms$nova(itemInHand, player, hand); // note that vanilla doesn't call use on entity interact
        return result;
    }
    
}
