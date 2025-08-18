package xyz.xenondevs.nova.mixin.item.repair;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z")
    @Definition(id = "getItem", method = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;")
    @Definition(id = "item1", local = @Local(type = ItemStack.class, ordinal = 2))
    @Expression("?.is(item1.getItem())")
    @Redirect(method = "createResult", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean isSameItemType(
        ItemStack first,
        Item secondType,
        @Local(ordinal = 2) ItemStack second
    ) {
        var firstNovaItem = ItemUtilsKt.getNovaItem(first);
        var secondNovaItem = ItemUtilsKt.getNovaItem(second);
        
        if (firstNovaItem != null || secondNovaItem != null)
            return firstNovaItem == secondNovaItem;
        
        return first.is(secondType);
    }
    
}
