package xyz.xenondevs.nova.mixin.block.correspondingitem;

import net.minecraft.world.item.Item;
import org.bukkit.craftbukkit.block.CraftBlockType;
import org.bukkit.inventory.ItemType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.world.item.NovaItemKt;

// performance-only mixin to use cached item type instance instead of looking it up
@Mixin(CraftBlockType.class)
abstract class CraftBlockTypeMixin {
    
    @Redirect(
        method = "getItemType",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/craftbukkit/inventory/CraftItemType;minecraftToBukkitNew(Lnet/minecraft/world/item/Item;)Lorg/bukkit/inventory/ItemType;"
        )
    )
    private ItemType getCachedItemType(Item minecraft) {
        return NovaItemKt.getItemType(minecraft);
    }
    
}
