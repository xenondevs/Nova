package xyz.xenondevs.nova.mixin.item.dyeable;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.behavior.Dyeable;

@Mixin(VillagerTrades.DyedArmorForEmeralds.class)
abstract class VillagerTradesDyedArmorForEmeraldsMixin {
    
    @Redirect(
        method = "getOffer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"
        )
    )
    private static boolean inject(ItemStack stack, TagKey<Item> tag) {
        return tag == ItemTags.DYEABLE ? Dyeable.isDyeable$nova(stack) : stack.is(tag);
    }
    
}
