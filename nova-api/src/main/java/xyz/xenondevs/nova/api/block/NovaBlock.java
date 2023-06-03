package xyz.xenondevs.nova.api.block;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.data.NamespacedId;
import xyz.xenondevs.nova.api.item.NovaItem;

public interface NovaBlock {
    
    /**
     * Gets the id of this block type.
     * @return The id of this block type.
     */
    @NotNull NamespacedId getId();
    
    /**
     * Gets the item for this block type, or null if there is none.
     * @return The item for this block type, or null if there is none.
     */
    @Nullable NovaItem getItem();
    
    /**
     * Gets the localized name of this block type.
     * @param locale The locale of the name. Should be in the same format as the language file names in
     *               resource packs (e.g. en_us).
     * @return The localized name of this block type.
     */
    @NotNull String getLocalizedName(String locale);
    
}
