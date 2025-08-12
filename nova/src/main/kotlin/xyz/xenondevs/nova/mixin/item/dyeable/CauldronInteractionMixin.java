package xyz.xenondevs.nova.mixin.item.dyeable;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.behavior.Dyeable;

@Mixin(CauldronInteraction.class)
interface CauldronInteractionMixin {
    
    @Redirect(
        method = "dyedItemIteration",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private static boolean isDyeable(ItemStack stack, TagKey<Item> tag) {
        return tag == ItemTags.DYEABLE ? Dyeable.isDyeable$nova(stack) : stack.is(tag);
    }
    
}
