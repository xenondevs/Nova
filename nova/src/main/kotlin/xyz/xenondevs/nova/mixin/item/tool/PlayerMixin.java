package xyz.xenondevs.nova.mixin.item.tool;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.util.item.ItemUtilsKt;
import xyz.xenondevs.nova.world.item.behavior.Tool;

@Mixin(Player.class)
abstract class PlayerMixin {
    
    @Definition(id = "is", method = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z")
    @Definition(id = "SWORDS", field = "Lnet/minecraft/tags/ItemTags;SWORDS:Lnet/minecraft/tags/TagKey;")
    @Expression("?.is(SWORDS)")
    @Redirect(method = "attack", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean canDoSweepAttack(ItemStack stack, TagKey<Item> swords) {
        var novaItem = ItemUtilsKt.getNovaItem(stack);
        if (novaItem != null) {
            var tool = novaItem.getBehaviorOrNull(Tool.class);
            return tool != null && tool.getCanSweepAttack();
        }
        
        return stack.is(swords);
    }
    
}
