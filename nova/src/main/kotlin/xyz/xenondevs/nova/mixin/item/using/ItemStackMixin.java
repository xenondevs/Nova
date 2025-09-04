package xyz.xenondevs.nova.mixin.item.using;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    
    @Redirect(
        method = "onUseTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;onUseTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V"
        )
    )
    private void redirectUseTickToNovaBehaviors(Item item, Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem != null) {
            novaItem.handleUseTick$nova(
                livingEntity.getBukkitLivingEntity(),
                CraftItemStack.asBukkitCopy(stack),
                NMSUtilsKt.getBukkitEquipmentSlot(livingEntity.getUsedItemHand()),
                livingEntity.getTicksUsingItem(),
                remainingUseDuration
            );
        } else {
            item.onUseTick(level, livingEntity, stack, remainingUseDuration);
        }
    }
    
    @ModifyReturnValue(method = "getUseDuration", at = @At("RETURN"))
    private int modifyUseDuration(int original, @Local(argsOnly = true) LivingEntity entity) {
        var thisRef = (ItemStack) (Object) this;
        var novaItem = ItemUtilsKt.getNovaItem(thisRef);
        if (novaItem == null)
            return original;
        
        return novaItem.modifyUseDuration$nova(
            entity.getBukkitLivingEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            original
        );
    }
    
    @ModifyReturnValue(method = "finishUsingItem", at = @At("RETURN"))
    private ItemStack handleFinishUseAndModifyRemainder(ItemStack remainder, @Local(argsOnly = true) LivingEntity entity) {
        var thisRef = (ItemStack) (Object) this;
        var novaItem = ItemUtilsKt.getNovaItem(thisRef);
        if (novaItem == null)
            return remainder;
        
        novaItem.handleUseFinished$nova(
            entity.getBukkitLivingEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            NMSUtilsKt.getBukkitEquipmentSlot(entity.getUsedItemHand())
        );
        return CraftItemStack.unwrap(novaItem.modifyUseRemainder$nova(
            entity.getBukkitLivingEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            NMSUtilsKt.getBukkitEquipmentSlot(entity.getUsedItemHand()),
            CraftItemStack.asBukkitCopy(remainder)
        )).copy();
    }
    
    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void handleUseStopped(Level level, LivingEntity livingEntity, int timeLeft, CallbackInfo ci) {
        var thisRef = (ItemStack) (Object) this;
        var novaItem = ItemUtilsKt.getNovaItem(thisRef);
        if (novaItem == null)
            return;
        
        novaItem.handleUseStopped$nova(
            livingEntity.getBukkitLivingEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            NMSUtilsKt.getBukkitEquipmentSlot(livingEntity.getUsedItemHand()),
            timeLeft
        );
    }
    
}
