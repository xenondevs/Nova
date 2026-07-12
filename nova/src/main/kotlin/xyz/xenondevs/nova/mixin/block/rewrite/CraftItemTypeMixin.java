package xyz.xenondevs.nova.mixin.block.rewrite;

import org.bukkit.block.BlockType;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.bukkit.inventory.ItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(CraftItemType.class)
abstract class CraftItemTypeMixin {
    
    @Inject(method = "hasBlockType", at = @At("HEAD"), cancellable = true)
    private void hasNovaBlockType(CallbackInfoReturnable<Boolean> cir) {
        if (!(((CraftItemType<?>) (Object) this).getHandle() instanceof NovaItem novaItem))
            return;
        cir.setReturnValue(novaItem.getBlock() != null);
    }
    
    @Inject(method = "getBlockType", at = @At("HEAD"), cancellable = true)
    private void getNovaBlockType(CallbackInfoReturnable<BlockType> cir) {
        if (!(((CraftItemType<?>) (Object) this).getHandle() instanceof NovaItem novaItem))
            return;
        var blockType = novaItem.getBlock();
        if (blockType == null)
            throw new IllegalStateException("The item type " + ((ItemType) this).getKey() + " has no corresponding block type");
        cir.setReturnValue(novaItem.getBlock());
    }
    
}
