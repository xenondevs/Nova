package xyz.xenondevs.nova.data.resources.layout.block

import org.bukkit.Material
import org.bukkit.block.data.BlockData
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.BrownMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MushroomStemBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.NoteBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig

/**
 * Represent the different block types whose states can be used to display a custom block model.
 *
 * State-backed custom block models are generally more performant than entity-backed models, but have some limitations.
 * There is also only a certain amount of total block states available that can be used for custom block models.
 *
 * @param fallbackHitbox The hitbox block type if display entities are used instead.
 */
enum class BackingStateCategory(
    internal val fallbackHitbox: BlockData,
    internal vararg val backingStateConfigTypes: BackingStateConfigType<*>
) {
    
    /**
     * The block model uses note block states.
     *
     * - Limited to 1149 different models
     * - Models cannot be transparent
     * - Client-side arm swing animation on right-click
     *
     */
    NOTE_BLOCK(
        fallbackHitbox = Material.BARRIER,
        NoteBackingStateConfig
    ),
    
    /**
     * The block model uses mushroom block states.
     *
     * - Limited to 189 different models
     * - Models cannot be transparent
     */
    MUSHROOM_BLOCK(
        fallbackHitbox = Material.BARRIER,
        RedMushroomBackingStateConfig, BrownMushroomBackingStateConfig, MushroomStemBackingStateConfig
    );
    
    constructor(fallbackHitbox: Material, vararg backingStateConfigTypes: BackingStateConfigType<*>) :
        this(fallbackHitbox.createBlockData(), *backingStateConfigTypes)
    
}