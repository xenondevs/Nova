package xyz.xenondevs.nova.mixin.item.dyeable;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ArmorDyeRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.behavior.Dyeable;

@Mixin(ArmorDyeRecipe.class)
abstract class ArmorDyeRecipeMixin {
    
    @Redirect(
        method = {
            "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;"
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private static boolean inject(ItemStack stack, TagKey<Item> tag) {
        return tag == ItemTags.DYEABLE ? Dyeable.isDyeable$nova(stack) : stack.is(tag);
    }
    
}
