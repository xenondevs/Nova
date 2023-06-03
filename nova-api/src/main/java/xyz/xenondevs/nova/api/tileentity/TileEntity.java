package xyz.xenondevs.nova.api.tileentity;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.block.NovaBlock;
import xyz.xenondevs.nova.api.material.NovaMaterial;

import java.util.List;

public interface TileEntity {
    
    /**
     * Gets the owner of this {@link TileEntity}.
     *
     * @return The owner of this {@link TileEntity}
     */
    @Nullable OfflinePlayer getOwner();
    
    /**
     * Gets the material of this {@link TileEntity}.
     *
     * @return The material of this {@link TileEntity}
     * @deprecated Use {@link #getBlock()} instead.
     */
    @Deprecated
    @NotNull NovaMaterial getMaterial();
    
    /**
     * Gets the {@link NovaBlock} of this {@link TileEntity}.
     *
     * @return The {@link NovaBlock} of this {@link TileEntity}
     */
    @NotNull NovaBlock getBlock();
    
    /**
     * Gets the {@link Location} of this {@link TileEntity}.
     *
     * @return The {@link Location} of this {@link TileEntity}
     */
    @NotNull Location getLocation();
    
    /**
     * Retrieves a list of all {@link ItemStack ItemStacks} this {@link TileEntity} would drop.
     *
     * @return A list of all {@link ItemStack ItemStacks} this {@link TileEntity} would drop.
     */
    @NotNull List<@NotNull ItemStack> getDrops(boolean includeSelf);
    
}