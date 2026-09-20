package xyz.xenondevs.nova.mixin.item.using;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.util.NMSUtilsKt;
import xyz.xenondevs.nova.world.InteractionResult;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
  
    // TODO: use tick in NovaItem
    
//    @Redirect(
//        method = "onUseTick",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/world/item/Item;onUseTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;I)V"
//        )
//    )
//    private void redirectUseTickToNovaBehaviors(Item item, Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
//        if (item instanceof NovaItem novaItem) {
//            novaItem.handleUseTick(
//                livingEntity.getBukkitLivingEntity(),
//                CraftItemStack.asBukkitCopy(stack),
//                NMSUtilsKt.getBukkitEquipmentSlot(livingEntity.getUsedItemHand()),
//                livingEntity.getTicksUsingItem(),
//                remainingUseDuration
//            );
//        } else {
//            item.onUseTick(level, livingEntity, stack, remainingUseDuration);
//        }
//    }
    
    // TODO: use duration in NovaItem
    
//    @ModifyReturnValue(method = "getUseDuration", at = @At("RETURN"))
//    private int modifyUseDuration(int original, @Local(argsOnly = true) LivingEntity entity) {
//        var thisRef = (ItemStack) (Object) this;
//        var novaItem = ItemUtilsKt.getNovaItem(thisRef);
//        if (novaItem == null)
//            return original;
//        
//        return novaItem.modifyUseDuration(
//            entity.getBukkitLivingEntity(),
//            CraftItemStack.asBukkitCopy(thisRef),
//            original
//        );
//    }
    
    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void handleFinishUseAndModifyRemainder(
        Level level,
        LivingEntity livingEntity,
        CallbackInfoReturnable<ItemStack> cir
    ) {
        var thisRef = (ItemStack) (Object) this;
        if (!(thisRef.getItem() instanceof NovaItem novaItem))
            return;
        
        var action = novaItem.handleUseFinished(
            livingEntity.getBukkitEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            NMSUtilsKt.getBukkitEquipmentSlot(livingEntity.getUsedItemHand())
        );
        new InteractionResult.Success(false, action).performActions(
            livingEntity.getBukkitEntity(),
            NMSUtilsKt.getBukkitEquipmentSlot(livingEntity.getUsedItemHand()),
            true
        );
        
        cir.setReturnValue(livingEntity.getItemInHand(livingEntity.getUsedItemHand()));
    }
    
    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void handleUseStopped(Level level, LivingEntity entity, int remainingTime, CallbackInfo ci) {
        var thisRef = (ItemStack) (Object) this;
        if (!(thisRef.getItem() instanceof NovaItem novaItem))
            return;
        
        novaItem.handleUseStopped(
            entity.getBukkitEntity(),
            CraftItemStack.asBukkitCopy(thisRef),
            NMSUtilsKt.getBukkitEquipmentSlot(entity.getUsedItemHand()),
            remainingTime
        );
    }
    
}
