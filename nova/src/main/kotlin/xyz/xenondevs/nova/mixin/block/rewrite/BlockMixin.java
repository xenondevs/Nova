package xyz.xenondevs.nova.mixin.block.rewrite;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.craftbukkit.inventory.CraftItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.xenondevs.nova.world.block.NovaBlock;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(Block.class)
abstract class BlockMixin {
    
    @Inject(method = "asItem", at = @At("HEAD"), cancellable = true)
    private void asNovaItem(CallbackInfoReturnable<Item> cir) {
        if (!((Block) (Object) this instanceof NovaBlock novaBlock))
            return;
        var itemEntry = novaBlock.getItem();
        cir.setReturnValue(
            itemEntry != null
                ? ((CraftItemType<?>) itemEntry.get()).getHandle()
                : Items.AIR
        );
    }
    
    @Inject(method = "byItem", at = @At("HEAD"), cancellable = true)
    private static void byNovaItem(Item item, CallbackInfoReturnable<Block> cir) {
        if (!(item instanceof NovaItem novaItem))
            return;
        var blockType = novaItem.getBlock();
        cir.setReturnValue(
            blockType != null
                ? ((CraftBlockType<?>) novaItem.getBlock()).getHandle()
                : Blocks.AIR
        );
    }
    
}
