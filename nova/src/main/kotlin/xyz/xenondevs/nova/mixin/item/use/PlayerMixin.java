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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.MixinContext;
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
        Entity entity,
        InteractionHand hand,
        Vec3 location,
        CallbackInfoReturnable<InteractionResult> cir,
        @Local(name = "itemStack") LocalRef<ItemStack> itemInHand,
        @Local(name = "itemStackClone") LocalRef<ItemStack> itemInHandClone
    ) {
        var player = (Player) (Object) this;
        itemInHand.set(player.getItemInHand(hand));
        itemInHandClone.set(itemInHand.get().copy());
    }
    
    @Redirect(
        method = "interactOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult skipEntityInteractionWhenSecondaryAndNovaItem(
        Entity entity,
        Player player,
        InteractionHand hand,
        Vec3 location
    ) {
        // skip entity interaction for sneak-clicking with nova items to align with the behavior of clicking blocks
        if (MixinContext.IS_USING_SECONDARY_ACTION.orElse(false) && ItemUtilsKt.getNovaItem(player.getItemInHand(hand)) != null)
            return InteractionResult.PASS;
        return entity.interact(player, hand, location);
    }
    
    @Redirect(
        method = "interactOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;interactLivingEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult useNovaItemOnLivingEntity(
        ItemStack stack,
        Player player,
        LivingEntity target,
        InteractionHand hand,
        @Local(argsOnly = true) Vec3 location
    ) {
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem == null)
            return stack.interactLivingEntity(player, target, hand);
        return novaItem.useOnEntityNms$nova(player, stack, target, hand, location);
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
        @Local(argsOnly = true) InteractionHand hand,
        @Local(argsOnly = true) Vec3 location
    ) {
        var player = (Player) (Object) this;
        if (player.isSpectator() || entity instanceof LivingEntity)
            return original;
        var itemInHand = player.getItemInHand(hand);
        var novaItem = ItemUtilsKt.getNovaItem(itemInHand);
        if (novaItem == null)
            return original;
        
        return novaItem.useOnEntityNms$nova(player, itemInHand, entity, hand, location);
    }
    
}
