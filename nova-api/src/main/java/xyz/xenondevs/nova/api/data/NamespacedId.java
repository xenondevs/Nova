package xyz.xenondevs.nova.api.data;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public interface NamespacedId {
    
    /**
     * Gets the namespace of this {@link NamespacedId}.
     *
     * @return The namespace of this{@link NamespacedId}.
     */
    @NotNull String getNamespace();
    
    /**
     * Gets the name of this {@link NamespacedId}.
     *
     * @return The name of this {@link NamespacedId}.
     */
    @NotNull String getName();
    
    /**
     * Creates a {@link NamespacedKey} with the namespace and name of this {@link NamespacedId}.
     *
     * @return The {@link NamespacedKey}.
     */
    @NotNull NamespacedKey toNamespacedKey();
    
}