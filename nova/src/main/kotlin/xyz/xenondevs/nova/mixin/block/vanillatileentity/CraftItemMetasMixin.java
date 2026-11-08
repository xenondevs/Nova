package xyz.xenondevs.nova.mixin.block.vanillatileentity;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.EntityBlock;
import org.bukkit.craftbukkit.inventory.CraftItemMetas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Prevent Cauldron VTE from being considered as EntityBlock for ItemMeta
// TODO: Reconsider approach, maybe making it a TE on bukkit-side makes more sense?
//       (Nova blocks / tile entities are currently not represented by ItemMeta either)
@Mixin(CraftItemMetas.class)
abstract class CraftItemMetasMixin {
    
    @Definition(id = "EntityBlock", type = EntityBlock.class)
    @Expression("? instanceof EntityBlock")
    @Redirect(
        method = "getItemMetaData",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private static boolean isActualEntityBlock(Object instance, Class<?> type) {
        return type.isInstance(instance) && !(instance instanceof AbstractCauldronBlock);
    }
    
}
