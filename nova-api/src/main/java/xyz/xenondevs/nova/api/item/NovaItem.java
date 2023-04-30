package xyz.xenondevs.nova.api.item;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.block.NovaBlock;
import xyz.xenondevs.nova.api.data.NamespacedId;

public interface NovaItem {
    
    /**
     * Gets the {@link NamespacedId} of this item.
     *
     * @return The {@link NamespacedId} of this item.
     */
    @NotNull NamespacedId getId();
    
    /**
     * Gets the {@link NovaBlock} this item is associated with, or null if there is none.
     *
     * @return The {@link NovaBlock} this item is associated with, or null if there is none.
     */
    @Nullable NovaBlock getBlock();
    
    /**
     * Gets the maximum stack size of this item.
     *
     * @return The maximum stack size of this item.
     */
    int getMaxStackSize();
    
    /**
     * Gets the localized name for this {@link NovaItem}.
     *
     * @param locale The locale to get the name in . Should be in the same format as the language file
     *               names in resource packs (e.g. en_us).
     * @return The localized name of this item for the specified locale.
     */
    @NotNull String getLocalizedName(String locale);
    
    /**
     * Creates an {@link ItemStack} of this {@link NovaItem} with the specified amount.
     * @param amount The amount of items in the stack.
     * @return An {@link ItemStack} of this {@link NovaItem} with the specified amount.
     */
    @NotNull ItemStack createItemStack(int amount);
    
    /**
     * Creates an {@link ItemStack} of this {@link NovaItem} with the amount of 1.
     * @return An {@link ItemStack} of this {@link NovaItem} with the amount of 1.
     */
    default @NotNull ItemStack createItemStack() {
        return createItemStack(1);
    }
    
    /**
     * Creates a client-side {@link ItemStack} of this {@link NovaItem} with the specified amount.
     * @param amount The amount of items in the stack.
     * @return A client-side {@link ItemStack} of this {@link NovaItem} with the specified amount.
     */
    @NotNull ItemStack createClientsideItemStack(int amount);
    
    /**
     * Creates a client-side {@link ItemStack} of this {@link NovaItem} with the amount of 1.
     * @return A client-side {@link ItemStack} of this {@link NovaItem} with the amount of 1.
     */
    default @NotNull ItemStack createClientsideItemStack() {
        return createClientsideItemStack(1);
    }
    
}
