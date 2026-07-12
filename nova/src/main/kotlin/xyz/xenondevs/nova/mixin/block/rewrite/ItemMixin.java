package xyz.xenondevs.nova.mixin.block.rewrite;

import net.minecraft.world.item.Item;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.xenondevs.commons.provider.Provider;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.nova.registry.RegistryEntry;

// see ItemTypeAccess.kt
@Mixin(Item.class)
abstract class ItemMixin {
    
    @Unique
    @Nullable
    public ItemType nova$cachedType;
    
    @Unique
    @Nullable
    public RegistryEntry.Paper<ItemType> nova$cachedTypeEntry;
    
    @Unique
    @Nullable
    public Provider<ItemProvider> nova$itemProvider;
    
    @Unique
    @Nullable
    public Provider<ItemProvider> nova$guiItemProvider;
    
}
