package xyz.xenondevs.nova.api.protection;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.xenondevs.nova.api.tileentity.TileEntity;

import static java.util.Objects.requireNonNull;

public interface ProtectionIntegration {
    
    /**
     * Specifies from which thread methods in this protection integration are allowed to be called.
     *
     * @return The {@link ExecutionMode}
     */
    default @NotNull ExecutionMode getExecutionMode() {
        return ExecutionMode.SERVER;
    }
    
    /**
     * Checks if that {@link OfflinePlayer} can break a block at that {@link Location} using that {@link ItemStack}.
     *
     * @param player   The player trying to break the block.
     * @param item     The item to break the block with.
     * @param location The location of the block.
     * @return If the player can break the block.
     */
    boolean canBreak(@NotNull OfflinePlayer player, @Nullable ItemStack item, @NotNull Location location);
    
    /**
     * Checks if that {@link TileEntity} can break a block at that {@link Location} using that {@link ItemStack}.
     *
     * @param tileEntity The tile-entity trying to break the block.
     * @param item       The item to break the block with.
     * @param location   The location of the block.
     * @return If the tile-entity can break the block.
     */
    default boolean canBreak(@NotNull TileEntity tileEntity, @Nullable ItemStack item, @NotNull Location location) {
        return canBreak(requireNonNull(tileEntity.getOwner()), item, location);
    }
    
    /**
     * Checks if that {@link OfflinePlayer} can place an {@link ItemStack} at that {@link Location}.
     *
     * @param player   The player trying to place the block.
     * @param item     The item to place.
     * @param location The location of the block.
     * @return If the player can place the block.
     */
    boolean canPlace(@NotNull OfflinePlayer player, @NotNull ItemStack item, @NotNull Location location);
    
    /**
     * Checks if that {@link TileEntity} can place an {@link ItemStack} at that {@link Location}.
     *
     * @param tileEntity The tile-entity trying to place the block.
     * @param item       The item to place.
     * @param location   The location of the block.
     * @return If the tile-entity can place the block.
     */
    default boolean canPlace(@NotNull TileEntity tileEntity, @NotNull ItemStack item, @NotNull Location location) {
        return canPlace(requireNonNull(tileEntity.getOwner()), item, location);
    }
    
    /**
     * Checks if the {@link OfflinePlayer} can interact with a block at that {@link Location} using that {@link ItemStack}.
     *
     * @param player   The player trying to interact with the block.
     * @param item     The item used to interact with the block.
     * @param location The location of the block.
     * @return If the player can interact with the block.
     */
    boolean canUseBlock(@NotNull OfflinePlayer player, @Nullable ItemStack item, @NotNull Location location);
    
    /**
     * Checks if the {@link TileEntity} can interact with a block at that {@link Location} using that {@link ItemStack}.
     *
     * @param tileEntity The tile-entity trying to interact with the block.
     * @param item       The item used to interact with the block.
     * @param location   The location of the block.
     * @return If the tile-entity can interact with the block.
     */
    default boolean canUseBlock(@NotNull TileEntity tileEntity, @Nullable ItemStack item, @NotNull Location location) {
        return canUseBlock(requireNonNull(tileEntity.getOwner()), item, location);
    }
    
    /**
     * Checks if the {@link OfflinePlayer} can use that {@link ItemStack} at that {@link Location}.
     *
     * @param player   The player trying to use the item.
     * @param item     The item the player tries to use.
     * @param location The location of the player.
     * @return If the player can use the item.
     */
    boolean canUseItem(@NotNull OfflinePlayer player, @NotNull ItemStack item, @NotNull Location location);
    
    /**
     * Checks if the {@link TileEntity} can use that {@link ItemStack} at that {@link Location}.
     *
     * @param tileEntity The tile-entity trying to use the item.
     * @param item       The item the player tries to use.
     * @param location   The location of the player.
     * @return If the tile-entity can use the item.
     */
    default boolean canUseItem(@NotNull TileEntity tileEntity, @NotNull ItemStack item, @NotNull Location location) {
        return canUseItem(requireNonNull(tileEntity.getOwner()), item, location);
    }
    
    /**
     * Checks if the {@link OfflinePlayer} can interact with the {@link Entity} using the {@link ItemStack}.
     *
     * @param player The player trying to interact with the entity.
     * @param entity The entity the player is trying to interact with.
     * @param item   The item the player is holding in their hand.
     * @return If the player can interact with the entity.
     */
    boolean canInteractWithEntity(@NotNull OfflinePlayer player, @NotNull Entity entity, @Nullable ItemStack item);
    
    /**
     * Checks if the {@link TileEntity} can interact with the {@link Entity} using the {@link ItemStack}.
     *
     * @param tileEntity The tile-entity trying to interact with the entity.
     * @param entity     The entity the player is trying to interact with.
     * @param item       The item the player is holding in their hand.
     * @return If the tile-entity can interact with the entity.
     */
    default boolean canInteractWithEntity(@NotNull TileEntity tileEntity, @NotNull Entity entity, @Nullable ItemStack item) {
        return canInteractWithEntity(requireNonNull(tileEntity.getOwner()), entity, item);
    }
    
    /**
     * Checks if the {@link OfflinePlayer} can hurt the {@link Entity} with this {@link ItemStack}
     *
     * @param player The player trying to hurt the entity.
     * @param entity The entity the player is trying to hurt.
     * @param item   The item the player is holding in their hand.
     * @return If the player can hurt the entity.
     */
    boolean canHurtEntity(@NotNull OfflinePlayer player, @NotNull Entity entity, @Nullable ItemStack item);
    
    /**
     * Checks if the {@link TileEntity} can hurt the {@link Entity} with this {@link ItemStack}
     *
     * @param tileEntity The tile-entity trying to hurt the entity.
     * @param entity     The entity the player is trying to hurt.
     * @param item       The item the player is holding in their hand.
     * @return If the tile-entity can hurt the entity.
     */
    default boolean canHurtEntity(@NotNull TileEntity tileEntity, @NotNull Entity entity, @Nullable ItemStack item) {
        return canHurtEntity(requireNonNull(tileEntity.getOwner()), entity, item);
    }
    
    /**
     * Defines how methods in this protection integration are allowed to be called
     */
    enum ExecutionMode {
        
        /**
         * The thread will not be changed in order to call the methods
         */
        NONE,
        
        /**
         * The methods are always called from the server thread
         */
        SERVER,
        
        /**
         * The methods are never called from the server thread
         */
        ASYNC
        
    }
    
}