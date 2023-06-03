package xyz.xenondevs.nova.api.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.data.NamespacedId;

import java.util.List;

public interface NovaItemRegistry {
    
    /**
     * Gets the {@link NovaItem} with the specified id.
     *
     * @param id The id of the item.
     * @return The {@link NovaItem} with the specified id.
     * @throws IllegalArgumentException If there is no {@link NovaItem} with the specified id.
     */
    @NotNull NovaItem get(@NotNull String id);
    
    /**
     * Gets the {@link NovaItem} with the specified id.
     *
     * @param id The id of the item.
     * @return The {@link NovaItem} with the specified id.
     * @throws IllegalArgumentException If there is no {@link NovaItem} with the specified id.
     */
    @NotNull NovaItem get(@NotNull NamespacedId id);
    
    /**
     * Gets the {@link NovaItem} with the specified id, or null if there is none.
     *
     * @param id The id of the item.
     * @return The {@link NovaItem} with the specified id, or null if there is none.
     */
    @Nullable NovaItem getOrNull(@NotNull String id);
    
    /**
     * Gets the {@link NovaItem} with the specified id, or null if there is none.
     *
     * @param id The id of the item.
     * @return The {@link NovaItem} with the specified id, or null if there is none.
     */
    @Nullable NovaItem getOrNull(@NotNull NamespacedId id);
    
    /**
     * Gets a list of {@link NovaItem NovaItems} with the specified name, ignoring the namespace.
     *
     * @param name The name of the item.
     * @return A list of {@link NovaItem NovaItems} with the specified name, ignoring the namespace.
     */
    @NotNull List<@NotNull NovaItem> getNonNamespaced(@NotNull String name);
    
}
