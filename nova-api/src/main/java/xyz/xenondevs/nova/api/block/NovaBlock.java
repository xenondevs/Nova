package xyz.xenondevs.nova.api.block;

import net.kyori.adventure.text.Component;
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
    @Deprecated
    default @NotNull String getLocalizedName(String locale) {
        return getPlaintextName(locale);
    }
    
    /**
     * Gets the name of this block type.
     * @return The name of this block type.
     */
    @NotNull Component getName();
    
    /**
     * Gets the plaintext name of this block type.
     * @param locale The locale to get the name in. Should be in the same format as the language file
     *               names in resource packs (e.g. en_us).
     * @return The name of this {@link NovaBlock} in plaintext.
     */
    @NotNull String getPlaintextName(@NotNull String locale);
    
}
