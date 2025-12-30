package xyz.xenondevs.nova.mixin.item.using;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(Item.class)
abstract class ItemMixin {
    
    @Definition(id = "PASS", field = "Lnet/minecraft/world/InteractionResult;PASS:Lnet/minecraft/world/InteractionResult$Pass;")
    @Expression("return PASS")
    @Inject(
        method = "use",
        at = @At(
            value = "MIXINEXTRAS:EXPRESSION",
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void novaUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        var itemStack = player.getItemInHand(hand);
        var novaItem = ItemUtilsKt.getNovaItem(itemStack);
        if (novaItem == null)
            return;
        
        var novaUseDuration = novaItem.modifyUseDuration(
            player.getBukkitEntity(),
            CraftItemStack.asBukkitCopy(itemStack),
            0
        );
        
        if (novaUseDuration > 0) {
            player.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
    
}
