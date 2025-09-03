package xyz.xenondevs.nova.mixin.item.remainder;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.item.NovaItem;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandBlockEntityMixin {
    
    @Inject(method = "doBrew", at = @At("HEAD"))
    private static void rememberBrewingNovaItem(
        Level level, 
        BlockPos pos,
        NonNullList<ItemStack> items, 
        BrewingStandBlockEntity brewingStand,
        CallbackInfo ci, 
        @Share("brewingNovaItem") LocalRef<NovaItem> brewingNovaItem
    ) {
        brewingNovaItem.set(ItemUtilsKt.getNovaItem(brewingStand.getItem(3)));
    }
    
    @Redirect(
        method = "doBrew",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;getCraftingRemainder()Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private static ItemStack getCraftingRemainer(
        Item item,
        @Share("brewingNovaItem") LocalRef<NovaItem> brewingNovaItem
    ) {
        var novaItem = brewingNovaItem.get();
        return novaItem != null
            ? CraftItemStack.unwrap(novaItem.getCraftingRemainingItem())
            : item.getCraftingRemainder();
    }
    
}
