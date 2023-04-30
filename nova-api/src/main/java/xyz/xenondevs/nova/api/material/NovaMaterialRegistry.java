package xyz.xenondevs.nova.api.material;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.block.NovaBlockRegistry;
import xyz.xenondevs.nova.api.data.NamespacedId;
import xyz.xenondevs.nova.api.item.NovaItemRegistry;

import java.util.List;

/**
 * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public interface NovaMaterialRegistry {
    
    /**
     * Gets the {@link NovaMaterial} of this id or throws an exception if there isn't one.
     *
     * @param id The id of the {@link NovaMaterial} in the format namespace:name
     * @return The {@link NovaMaterial}
     * @throws NullPointerException If there is no {@link NovaMaterial} of that id.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @NotNull NovaMaterial get(@NotNull String id);
    
    /**
     * Gets the {@link NovaMaterial} of this id or throws an exception if there isn't one.
     * @param id The id of the {@link NovaMaterial}.
     * @return The {@link NovaMaterial}.
     * @throws NullPointerException If there is no {@link NovaMaterial} of that id.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @NotNull NovaMaterial get(@NotNull NamespacedId id);
    
    /**
     * Gets the {@link NovaMaterial} of this {@link ItemStack} or throws an exception if
     * this {@link ItemStack} is not from Nova.
     *
     * @param item The {@link ItemStack} to get the {@link NovaMaterial} for.
     * @return The {@link NovaMaterial} of this {@link ItemStack}.
     * @throws NullPointerException If there is no {@link NovaMaterial} for that id.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @NotNull NovaMaterial get(@NotNull ItemStack item);
    
    /**
     * Gets the {@link NovaMaterial} of this id or null if there isn't one.
     *
     * @param id The id of the {@link NovaMaterial} in the format namespace:name.
     * @return The {@link NovaMaterial}.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @Nullable NovaMaterial getOrNull(@NotNull String id);
    
    /**
     * Gets the {@link NovaMaterial} of this id or null if there isn't one.
     * @param id The id of the {@link NovaMaterial}.
     * @return The {@link NovaMaterial}.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @Nullable NovaMaterial getOrNull(@NotNull NamespacedId id);
    
    /**
     * Gets the {@link NovaMaterial} of this {@link ItemStack} or null if there isn't one.
     *
     * @param item The {@link ItemStack} to get the {@link NovaMaterial} for.
     * @return The {@link NovaMaterial} of this {@link ItemStack}.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @Nullable NovaMaterial getOrNull(@NotNull ItemStack item);
    
    /**
     * Gets a list of {@link NovaMaterial NovaMaterials} registered under this name in all Nova namespaces.
     *
     * @param name The name of the item without the namespace.
     * @return A list of all {@link NovaMaterial NovaMaterials} under all Nova namespaces with that name.
     * @deprecated Use {@link NovaBlockRegistry} and {@link NovaItemRegistry} instead.
     */
    @Deprecated
    @NotNull List<@NotNull NovaMaterial> getNonNamespaced(@NotNull String name);
    
}