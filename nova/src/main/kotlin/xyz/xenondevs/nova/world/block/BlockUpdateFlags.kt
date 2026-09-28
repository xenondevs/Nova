package xyz.xenondevs.nova.world.block

import net.minecraft.world.level.block.Block

/**
 * Flags that control notifications and side effects of a block-state change.
 *
 * Flags can be combined using [plus] and removed using [minus].
 */
@JvmInline
value class BlockUpdateFlags(val value: Int) {
    
    operator fun plus(other: BlockUpdateFlags): BlockUpdateFlags =
        BlockUpdateFlags(value or other.value)
    
    operator fun minus(other: BlockUpdateFlags): BlockUpdateFlags =
        BlockUpdateFlags(value and other.value.inv())
    
    operator fun contains(flag: BlockUpdateFlags): Boolean =
        value and flag.value == flag.value
    
    companion object {
        
        // -- Individual Flags --
        
        /**
         * Notifies neighboring blocks of the change and updates adjacent comparator output.
         */
        val NOTIFY_NEIGHBORS = BlockUpdateFlags(Block.UPDATE_NEIGHBORS)
        
        /**
         * Notifies level listeners of the change.
         *
         * On the server, this sends the block update to clients and updates affected pathfinding data.
         */
        val NOTIFY_CLIENTS = BlockUpdateFlags(Block.UPDATE_CLIENTS)
        
        /**
         * Skips the render update caused by a client-side block update.
         *
         * This only has an effect on a client level and is only relevant together with [NOTIFY_CLIENTS].
         */
        val SKIP_RENDER_UPDATE = BlockUpdateFlags(Block.UPDATE_INVISIBLE)
        
        /**
         * Performs a client-side render update immediately instead of deferring it.
         *
         * This only has an effect on a client level and is only relevant together with [NOTIFY_CLIENTS].
         */
        val IMMEDIATE_RENDER_UPDATE = BlockUpdateFlags(Block.UPDATE_IMMEDIATE)
        
        /**
         * Skips propagating shape updates from the old and new block states to neighboring blocks.
         *
         * Use this only when no shape updates are required or when they are handled separately.
         */
        val SKIP_SHAPE_UPDATES = BlockUpdateFlags(Block.UPDATE_KNOWN_SHAPE)
        
        /**
         * Skips item drops when shape-update logic destroys a block.
         */
        val SKIP_DROPS = BlockUpdateFlags(Block.UPDATE_SUPPRESS_DROPS)
        
        /**
         * Marks the state change as part of a piston move.
         *
         * This affects removal and placement handling and causes removal-neighbor updates even without
         * [NOTIFY_NEIGHBORS].
         */
        val MOVE_BY_PISTON = BlockUpdateFlags(Block.UPDATE_MOVE_BY_PISTON)
        
        /**
         * Skips shape updates for redstone wire without skipping them for other neighboring blocks.
         */
        val SKIP_SHAPE_UPDATE_ON_WIRE = BlockUpdateFlags(Block.UPDATE_SKIP_SHAPE_UPDATE_ON_WIRE)
        
        /**
         * Skips [net.minecraft.world.level.block.entity.BlockEntity.preRemoveSideEffects] when replacing a
         * block entity.
         *
         * The block entity itself is still removed.
         */
        val SKIP_BLOCK_ENTITY_SIDE_EFFECTS = BlockUpdateFlags(Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS)
        
        /**
         * Skips [net.minecraft.world.level.block.state.BlockState.onPlace] for the new block state.
         */
        val SKIP_ON_PLACE = BlockUpdateFlags(Block.UPDATE_SKIP_ON_PLACE)
        
        /**
         * Skips updating the world's point-of-interest data.
         */
        val SKIP_POI = BlockUpdateFlags(Block.UPDATE_SKIP_POI)
        
        /**
         * Performs the state change without notifying neighbors or clients.
         *
         * This preset also includes [SKIP_RENDER_UPDATE] and [SKIP_BLOCK_ENTITY_SIDE_EFFECTS]. Its numeric value
         * is therefore not zero.
         */
        val NONE = BlockUpdateFlags(Block.UPDATE_NONE)
        
        // -- Presets --
        
        /**
         * Matches Bukkit's block-state update flags when `applyPhysics` is `false`.
         *
         * Notifies clients while skipping shape updates and block-entity removal side effects.
         */
        val BUKKIT_NO_PHYSICS = NOTIFY_CLIENTS + SKIP_SHAPE_UPDATES + SKIP_BLOCK_ENTITY_SIDE_EFFECTS
        
        /**
         * Notifies neighbors and clients.
         */
        val ALL = BlockUpdateFlags(Block.UPDATE_ALL)
        
        /**
         * Notifies neighbors and clients and performs the client-side render update immediately.
         */
        val ALL_IMMEDIATE = BlockUpdateFlags(Block.UPDATE_ALL_IMMEDIATE)
        
        /**
         * Skips shape updates, item drops, block-entity removal side effects, and placement handling.
         *
         * This does not include [SKIP_POI].
         */
        val SKIP_ALL_SIDE_EFFECTS = BlockUpdateFlags(Block.UPDATE_SKIP_ALL_SIDEEFFECTS)
        
    }
    
}
