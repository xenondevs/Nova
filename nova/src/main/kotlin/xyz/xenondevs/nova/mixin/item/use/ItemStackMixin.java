package xyz.xenondevs.nova.mixin.item.use;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.MixinContexts;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    
    @Redirect(
        method = "interactLivingEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;interactLivingEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult useNovaItemOnLivingEntity(
        Item item,
        ItemStack stack,
        Player player,
        LivingEntity target,
        InteractionHand hand
    ) {
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem == null)
            return item.interactLivingEntity(stack, player, target, hand);
        var result = novaItem.useOnEntityNms$nova(player, stack, target, hand, MixinContexts.INTERACT_LOCATION.get());
        if (result instanceof InteractionResult.Pass)
            result = novaItem.useNms$nova(stack, player, hand); // note that vanilla doesn't call use on entity interact
        return result;
    }
    
}
