package xyz.xenondevs.nova.mixin.item.use;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    
    @Redirect(
        method = "useItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult itemStackUseOn(
        ItemStack itemStack,
        UseOnContext context
    ) {
        if (!(itemStack.getItem() instanceof NovaItem novaItem))
            return itemStack.useOn(context);
        return novaItem.useOnBlockNms$nova(itemStack.copy(), context);
    }
    
    @Redirect(
        method = "useItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;use(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult itemStackUse(
        ItemStack itemStack,
        Level level,
        Player player,
        InteractionHand hand
    ) {
        if (!(itemStack.getItem() instanceof NovaItem novaItem))
            return itemStack.use(level, player, hand);
        return novaItem.useNms$nova(itemStack.copy(), player, hand);
    }
    
}
