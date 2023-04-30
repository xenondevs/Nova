package xyz.xenondevs.nova.api.material;

import xyz.xenondevs.nova.api.block.NovaBlock;
import xyz.xenondevs.nova.api.data.NamespacedId;
import xyz.xenondevs.nova.api.item.NovaItem;

/**
 * @deprecated Use {@link NovaBlock} and {@link NovaItem} instead.
 */
@Deprecated
public interface NovaMaterial {
    
    /**
     * Gets the {@link NamespacedId} of this {@link NovaMaterial}.
     *
     * @return The {@link NamespacedId} of this {@link NovaMaterial}
     * @deprecated Use {@link NovaBlock} and {@link NovaItem} instead.
     */
    @Deprecated
    NamespacedId getId();
    
    /**
     * Gets the localized name for this {@link NovaMaterial}.
     *
     * @param locale The locale to use. Should be in the same format as the language file names in
     *               resource packs (e.g. en_us).
     * @return The localized name.
     * @deprecated Use {@link NovaBlock} and {@link NovaItem} instead.
     */
    @Deprecated
    String getLocalizedName(String locale);
    
}