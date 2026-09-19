package xyz.xenondevs.nova.mixin.item.bukkit;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CraftMagicNumbers.class)
abstract class CraftMagicNumbersMixin {
    
    @ModifyReturnValue(
        method = "getMaterial(Lnet/minecraft/world/item/Item;)Lorg/bukkit/Material;",
        at = @At("RETURN")
    )
    private static Material useShulkerShellForCustomItems(Material original) {
        if (original == null)
            return Material.SHULKER_SHELL;
        return original;
    }
    
}
    
