package xyz.xenondevs.nova.mixin.item.remainder;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockMixin {
    
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void rememberSmeltingNovaItem(
        ServerLevel level,
        BlockPos pos,
        BlockState state,
        AbstractFurnaceBlockEntity furnace,
        CallbackInfo ci,
        @Share("smeltingNovaItem") LocalRef<NovaItem> smeltingNovaItem
    ) {
        smeltingNovaItem.set(ItemUtilsKt.getNovaItem(furnace.getItem(1)));
    }
    
    @Redirect(
        method = "serverTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getCraftingRemainder()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack getCraftingRemainer(
        Item item,
        @Share("smeltingNovaItem") LocalRef<NovaItem> smeltingNovaItem
    ) {
        var novaItem = smeltingNovaItem.get();
        return novaItem != null
            ? CraftItemStack.unwrap(novaItem.getCraftingRemainingItem())
            : item.getCraftingRemainder();
    }
    
}
